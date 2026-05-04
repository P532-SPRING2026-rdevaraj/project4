const API_BASE = (window.API_BASE || '').replace(/\/$/, '');
async function parseOrThrow(r) {
  const text = await r.text();
  let body;
  try { body = text ? JSON.parse(text) : null; }
  catch { throw new Error(`HTTP ${r.status}: ${text.slice(0, 200) || r.statusText}`); }
  if (!r.ok) throw new Error(body?.message || body?.error || `HTTP ${r.status}`);
  return body;
}
const api = {
  get: (url) => fetch(API_BASE + url).then(parseOrThrow),
  post: (url, body) => fetch(API_BASE + url, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: body == null ? null : JSON.stringify(body),
  }).then(parseOrThrow),
};

document.querySelectorAll('nav button').forEach((btn) => {
  btn.addEventListener('click', () => activate(btn.dataset.tab));
});

function activate(name) {
  document.querySelectorAll('nav button').forEach((b) => b.classList.toggle('active', b.dataset.tab === name));
  document.querySelectorAll('.tab').forEach((s) => s.classList.toggle('active', s.id === name));
  if (name === 'dashboard') loadDashboard();
  if (name === 'protocols') loadProtocols();
  if (name === 'resources') loadResources();
  if (name === 'plans') loadPlans();
  if (name === 'ledger') loadLedger();
  if (name === 'audit') loadAudit();
}

// --- Dashboard ----------------------------------------------------------------
async function loadDashboard() {
  const accounts = await api.get('/api/accounts');
  const pools = accounts.filter((a) => a.kind === 'POOL');
  const cards = document.getElementById('dashboard-cards');
  cards.innerHTML = '';
  pools.forEach((a) => {
    const div = document.createElement('div');
    const negative = parseFloat(a.balance) < 0;
    div.className = 'card' + (negative ? ' alert' : '');
    div.innerHTML = `
      <h3>${a.name.replace(/^pool:/, '')}</h3>
      <p><strong>${a.balance}</strong> ${a.unit ?? ''}</p>
      ${negative ? '<p>⚠ Over-consumed</p>' : ''}`;
    cards.appendChild(div);
  });
}

// --- Protocols ----------------------------------------------------------------
async function loadProtocols() {
  const list = await api.get('/api/protocols');
  const ul = document.getElementById('protocol-list');
  ul.innerHTML = '';
  list.forEach((p) => {
    const li = document.createElement('li');
    li.textContent = `#${p.id} ${p.name} — ${p.steps?.length ?? 0} step(s)`;
    ul.appendChild(li);
  });
}

document.getElementById('protocol-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const fd = new FormData(e.target);
  const stepsRaw = (fd.get('steps') || '').toString().trim();
  const steps = stepsRaw ? stepsRaw.split(/\n+/).map((line) => {
    const [stepName, depsRaw] = line.split('|');
    const dependsOn = depsRaw ? depsRaw.split(',').map((s) => s.trim()).filter(Boolean) : [];
    return { stepName: stepName.trim(), subProtocolId: null, dependsOn };
  }) : [];
  await api.post('/api/protocols', {
    name: fd.get('name'), description: fd.get('description'), steps,
  });
  e.target.reset();
  loadProtocols();
});

// --- Resource types -----------------------------------------------------------
async function loadResources() {
  const list = await api.get('/api/resource-types');
  const ul = document.getElementById('resource-list');
  ul.innerHTML = '';
  list.forEach((rt) => {
    const li = document.createElement('li');
    li.textContent = `#${rt.id} ${rt.name} (${rt.kind}, unit: ${rt.unit}) — pool balance: ${rt.poolAccount?.balance ?? '0'}`;
    ul.appendChild(li);
  });
}

document.getElementById('resource-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const fd = new FormData(e.target);
  try {
    await api.post('/api/resource-types', {
      name: fd.get('name'),
      kind: fd.get('kind'),
      unit: fd.get('unit'),
      initialPoolBalance: parseFloat(fd.get('initialPoolBalance') || '0'),
    });
    e.target.reset();
    loadResources();
  } catch (err) {
    alert(`Could not create resource type: ${err.message}`);
  }
});

// --- Plans --------------------------------------------------------------------
let plansCache = [];

async function loadPlans() {
  const protocols = await api.get('/api/protocols');
  const sel = document.querySelector('#plan-form select[name=protocolId]');
  sel.innerHTML = '<option value="">(scratch)</option>'
    + protocols.map((p) => `<option value="${p.id}">${p.name}</option>`).join('');
  plansCache = await api.get('/api/plans');
  const ul = document.getElementById('plan-list');
  ul.innerHTML = '';
  plansCache.forEach((p) => {
    const li = document.createElement('li');
    li.innerHTML = `<strong>${p.name}</strong> <span class="status ${p.status}">${p.status}</span>`;
    li.addEventListener('click', () => showPlan(p.id));
    ul.appendChild(li);
  });
}

document.getElementById('plan-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const fd = new FormData(e.target);
  await api.post('/api/plans', {
    name: fd.get('name'),
    protocolId: fd.get('protocolId') ? parseInt(fd.get('protocolId'), 10) : null,
  });
  e.target.reset();
  loadPlans();
});

let currentPlanId = null;

async function showPlan(id) {
  currentPlanId = id;
  const [detail, report, metrics] = await Promise.all([
    api.get(`/api/plans/${id}`),
    api.get(`/api/plans/${id}/report`),
    api.get(`/api/plans/${id}/metrics`),
  ]);

  document.getElementById('plan-detail').classList.remove('hidden');

  const depthSlider = document.getElementById('depth-slider');
  const depthLabel = document.getElementById('depth-label');
  depthSlider.oninput = () => {
    depthLabel.textContent = depthSlider.value;
    renderTreeWithDepth(detail, parseInt(depthSlider.value, 10));
  };
  renderTreeWithDepth(detail, parseInt(depthSlider.value, 10));

  const metricsPanel = document.getElementById('plan-metrics');
  metricsPanel.classList.remove('hidden');
  document.getElementById('metric-ratio').textContent =
    `Completion: ${(metrics.completionRatio * 100).toFixed(1)}% (${metrics.completedLeaves}/${metrics.totalLeaves} leaves)`;
  document.getElementById('metric-cost').textContent =
    ` | Cost: ${metrics.totalResourceCost}`;
  document.getElementById('metric-risk').textContent =
    ` | Risk score: ${metrics.riskScore}`;

  const filter = document.getElementById('report-filter');
  const renderReport = async () => {
    const sf = filter.value;
    const r = await api.get(`/api/plans/${id}/report${sf ? '?statusFilter=' + sf : ''}`);
    document.querySelector('#plan-report tbody').innerHTML = r.rows.map((row) => `
      <tr>
        <td>${row.name}</td>
        <td>${row.type}</td>
        <td><span class="status ${row.status}">${row.status}</span></td>
        <td>${Object.entries(row.totals).map(([k, v]) => `${k}: ${v}`).join('; ') || '—'}</td>
      </tr>`).join('');
  };
  filter.onchange = renderReport;
  document.querySelector('#plan-report tbody').innerHTML = report.rows.map((r) => `
    <tr>
      <td>${r.name}</td>
      <td>${r.type}</td>
      <td><span class="status ${r.status}">${r.status}</span></td>
      <td>${Object.entries(r.totals).map(([k, v]) => `${k}: ${v}`).join('; ') || '—'}</td>
    </tr>`).join('');
}

function renderTreeWithDepth(node, depth) {
  document.getElementById('plan-tree').innerHTML = renderTree(node, 0, depth);
  document.getElementById('plan-tree').addEventListener('click', onTreeClick);
}

function renderTree(node, depth = 0, depthLimit = 10) {
  if (node.type === 'ACTION') {
    return `<div data-action-id="${node.id}" class="tree-leaf">▸ ${node.name} <span class="status ${node.status}">${node.status}</span></div>`;
  }
  if (depth >= depthLimit) {
    return `<div class="tree-leaf collapsed" data-plan-id="${node.id}">▶ ${node.name} <span class="status ${node.status}">${node.status}</span> <em>(collapsed)</em></div>`;
  }
  return `<details open>
    <summary>${node.name} <span class="status ${node.status}">${node.status}</span></summary>
    <div class="tree">${node.children.map((c) => renderTree(c, depth + 1, depthLimit)).join('')}</div>
  </details>`;
}

function onTreeClick(e) {
  const leaf = e.target.closest('.tree-leaf');
  if (!leaf) return;
  showAction(parseInt(leaf.dataset.actionId, 10));
}

const TRANSITIONS = {
  PROPOSED: ['submit-for-approval', 'suspend', 'abandon'],
  PENDING_APPROVAL: ['approve', 'reject'],
  SUSPENDED: ['resume', 'abandon'],
  IN_PROGRESS: ['complete', 'suspend', 'abandon'],
  COMPLETED: ['reopen'],
  REOPENED: ['complete', 'abandon'],
  ABANDONED: [],
};
const ALL_OPS = ['submit-for-approval','approve','reject','implement','complete','suspend','resume','abandon','reopen'];

async function showAction(id) {
  const a = await api.get(`/api/actions/${id}`);
  const allowed = TRANSITIONS[a.status] || [];
  document.getElementById('action-detail').classList.remove('hidden');
  document.getElementById('action-content').innerHTML = `
    <p><strong>${a.name}</strong> <span class="status ${a.status}">${a.status}</span></p>
    <div class="actions-bar">
      ${ALL_OPS.map((op) => `
        <button data-op="${op}" data-id="${a.id}" ${allowed.includes(op) ? '' : 'disabled'}>${op}</button>`).join('')}
    </div>
    <div class="diff">
      <div><h4>Plan</h4>
        <p>Party: ${a.party ?? '—'}</p>
        <p>Location: ${a.location ?? '—'}</p>
        <p>Time ref: ${a.timeRef ?? '—'}</p>
      </div>
      <div><h4>Reality</h4>
        ${a.implemented ? `
          <p>Actual party: ${a.implemented.actualParty ?? '—'}</p>
          <p>Actual location: ${a.implemented.actualLocation ?? '—'}</p>
          <p>Actual start: ${a.implemented.actualStart ?? '—'}</p>` : '<p>(not implemented)</p>'}
      </div>
    </div>
    <h4>Allocate resource</h4>
    <form id="alloc-form">
      <label>Resource type id <input name="resourceTypeId" type="number" required /></label>
      <label>Quantity <input name="quantity" type="number" step="0.01" required /></label>
      <label>Kind <select name="kind"><option>GENERAL</option><option>SPECIFIC</option></select></label>
      <label>Asset id <input name="assetId" /></label>
      <button type="submit">Add allocation</button>
    </form>`;

  document.querySelectorAll('.actions-bar button').forEach((btn) => {
    btn.addEventListener('click', async () => {
      const op = btn.dataset.op;
      const url = `/api/actions/${btn.dataset.id}/${op}`;
      const body = op === 'suspend' ? { reason: prompt('Reason?') || 'no reason given' } : null;
      try {
        await api.post(url, body);
        showAction(parseInt(btn.dataset.id, 10));
        loadPlans();
      } catch (e) { alert(`Error: ${e.message || JSON.stringify(e)}`); }
    });
  });
  document.getElementById('alloc-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    try {
      await api.post(`/api/actions/${id}/allocations`, {
        resourceTypeId: parseInt(fd.get('resourceTypeId'), 10),
        quantity: parseFloat(fd.get('quantity')),
        kind: fd.get('kind'),
        assetId: fd.get('assetId') || null,
      });
      showAction(id);
    } catch (err) {
      alert(`Could not add allocation: ${err.message}`);
    }
  });
}

// --- Ledger -------------------------------------------------------------------
async function loadLedger() {
  const accounts = await api.get('/api/accounts');
  const sel = document.getElementById('ledger-account');
  sel.innerHTML = accounts.map((a) => `<option value="${a.id}">${a.name} (${a.kind})</option>`).join('');
  if (accounts.length) loadLedgerEntries(accounts[0].id);
  sel.onchange = () => loadLedgerEntries(parseInt(sel.value, 10));
  document.getElementById('ledger-filter').onchange = () => loadLedgerEntries(parseInt(sel.value, 10));
}

async function loadLedgerEntries(accountId) {
  const entries = await api.get(`/api/accounts/${accountId}/entries`);
  const filterVal = document.getElementById('ledger-filter')?.value ?? 'all';
  const filtered = entries.filter((e) => {
    if (filterVal === 'consumable') return e.accountName?.startsWith('usage:');
    if (filterVal === 'asset') return e.accountName?.startsWith('asset-usage:');
    return true;
  });
  const tbody = document.querySelector('#ledger-table tbody');
  tbody.innerHTML = filtered.map((e) => `
    <tr>
      <td>${e.bookedAt}</td>
      <td>${e.chargedAt}</td>
      <td>${e.amount}</td>
      <td>${e.accountName ?? '—'}</td>
      <td>${e.originatingActionName ?? '—'} (#${e.originatingActionId ?? '—'})</td>
    </tr>`).join('');
}

// --- Audit log ----------------------------------------------------------------
async function loadAudit() {
  const log = await api.get('/api/audit-log');
  const tbody = document.querySelector('#audit-table tbody');
  tbody.innerHTML = log.map((e) => `
    <tr><td>${e.timestamp}</td><td>${e.event}</td><td>${e.detail ?? ''}</td></tr>`).join('');
}

loadDashboard();
