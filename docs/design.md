# Resource Planning Ledger — Design Document (Week 1)

## 1. Architecture: four-layer split

```
┌─────────────────────────────────────────────────────────────┐
│ Client       (@RestController)                              │
│   ProtocolController · ResourceTypeController ·             │
│   PlanController · ActionController · AccountController ·   │
│   AuditLogController · GlobalExceptionHandler               │
├─────────────────────────────────────────────────────────────┤
│ Manager      (@Service)                                     │
│   ProtocolManager · ResourceTypeManager · PlanManager ·     │
│   PlanReportManager · ActionManager · LedgerManager ·       │
│   AuditManager                                              │
├─────────────────────────────────────────────────────────────┤
│ Engine       (algorithms — replaceable)                     │
│   state/     ActionState + 5 concrete states + Registry +   │
│              ActionContext + ActionStateCallbacks           │
│   composite/ PlanNode + PlanNodeVisitor                     │
│   iterator/  DepthFirstPlanIterator                         │
│   ledger/    AbstractLedgerEntryGenerator (template)        │
│              ConsumableLedgerEntryGenerator (Week-1 leaf)   │
│   posting/   PostingRuleEngine (over-consumption alert)     │
├─────────────────────────────────────────────────────────────┤
│ ResourceAccess (@Repository — Spring Data JPA)              │
│   ProtocolRepository · ResourceTypeRepository ·             │
│   PlanRepository · ProposedActionRepository ·               │
│   ImplementedActionRepository · SuspensionRepository ·      │
│   ResourceAllocationRepository · AccountRepository ·        │
│   TransactionRepository · EntryRepository ·                 │
│   PostingRuleRepository · AuditLogEntryRepository           │
└─────────────────────────────────────────────────────────────┘
```

Strict invariants:
- Controllers contain HTTP plumbing only — no business logic.
- Engines never call other engines (they only know domain types and their own collaborators).
- Repositories never publish events.

## 2. UML class diagram (text rendering)

```
                    ┌──────────────┐
                    │  PlanNode    │◄──────────────┐
                    │ (interface)  │               │
                    │ +getStatus() │               │
                    │ +accept(v)   │               │
                    └─────▲────────┘               │
              ┌───────────┴───────────┐    ┌──────────────────┐
              │                       │    │ PlanNodeVisitor  │
       ┌──────┴──────┐         ┌──────┴────┴┐ (interface)     │
       │ ProposedAct │         │   Plan      │  visit(Plan)   │
       │  (leaf)     │         │ (composite) │  visit(Action) │
       │ status:Enum │         │ children: * │                │
       └──────┬──────┘         └──────┬──────┘                 
              │                       │                         
              │ allocations *         │ children *              
              ▼                       ▼                         
       ┌──────────────┐        ┌──────────────┐                
       │ ResourceAlloc│        │   PlanNode   │                
       └──────────────┘        └──────────────┘                
              ▲                                                 
              │ uses                                            
       ┌──────┴───────────────────────┐                         
       │ ActionContext                │                         
       │  -action: ProposedAction     │                         
       │  -callbacks                  │                         
       └──────┬───────────────────────┘                         
              │                                                 
              ▼                                                 
       ┌──────────────────────┐                                 
       │ ActionState (iface)  │   one stateless singleton       
       │  +implement(ctx)     │   per concrete state            
       │  +suspend(ctx,r)     │                                 
       │  +resume(ctx)        │                                 
       │  +complete(ctx)      │                                 
       │  +abandon(ctx)       │                                 
       └──┬──┬──┬──┬──┬───────┘                                 
          │  │  │  │  │                                         
   Proposed Suspended InProgress Completed Abandoned            

       ┌────────────────────────────────────┐
       │ AbstractLedgerEntryGenerator       │
       │  +generateEntries(impl) FINAL      │  ← skeleton
       │  #selectAllocations(impl) ABSTRACT │
       │  #validate(allocs)        ABSTRACT │
       │  #buildWithdrawal(...)             │
       │  #buildDeposit(...)                │
       │  #afterPost(tx)           HOOK     │
       │  -postEntries(...)        FINAL    │  ← conservation check
       └─────────────▲──────────────────────┘
                     │
       ┌─────────────┴──────────────────────┐
       │ ConsumableLedgerEntryGenerator     │
       │  selects CONSUMABLE allocs only    │
       │  validates positive quantity       │
       └────────────────────────────────────┘
                                                                
       ┌──────────────────────┐    ┌────────────────────┐
       │  Iterator<PlanNode>  │◄───│ DepthFirst         │
       └──────────────────────┘    │ PlanIterator       │
                                   │  (pure Java, no    │
                                   │   DB calls)        │
                                   └────────────────────┘
```

The full entity ER is conventional Spring Data JPA: `Plan ↔ Plan (parent_plan)`, `Plan 1:* ProposedAction (parent_plan)`, `ProposedAction 1:* ResourceAllocation`, `ProposedAction 1:1 ImplementedAction`, `Transaction 1:* Entry`, `Entry *:1 Account`, `ResourceType 1:1 Account (pool)`.

## 3. Knowledge vs. operational split

- **Knowledge level** (rarely changed, never mutated by execution): `Protocol`, `ProtocolStep`, `ResourceType`. Owned by `ProtocolManager` and `ResourceTypeManager`.
- **Operational level**: `Plan`, `ProposedAction`, `ImplementedAction`, `Suspension`, `Transaction`, `Entry`, `AuditLogEntry`. Owned by `PlanManager`, `ActionManager`, `LedgerManager`, `AuditManager`.

Plans copy *information* from a Protocol at creation time; nothing in plan execution writes back into a `Protocol` row.

## 4. Justification of pattern choices

### State — ActionStateMachine
- Each `ActionState` is a stateless `@Component` singleton; mutable data lives only in `ActionContext`. State objects are mapped to `ActionStatus` values by `ActionStateRegistry`, so the persisted enum is the source of truth.
- **Why** State and not a `switch`: Week 2's lifecycle changes (e.g. a `PARTIALLY_COMPLETED` state) become *one new class plus one map entry plus one outgoing transition edit*. Branching code in the Manager would explode combinatorially.
- Side-effects (creating an `ImplementedAction`, posting ledger entries, opening/closing `Suspension`) are exposed via `ActionStateCallbacks`, implemented by `ActionManager`. This keeps states free of repository/Spring dependencies and trivially unit-testable.

### Composite — PlanNode tree
- `PlanNode` is implemented by both `ProposedAction` (leaf) and `Plan` (composite). `Plan#getStatus()` is *derived* by walking children — no field on `Plan`.
- **Why**: F10 reports must traverse trees uniformly. Without Composite, the report code would `instanceof`-check every node. With it, the same `getStatus()` / `getTotalAllocatedQuantity()` work on a leaf or a sub-plan.
- Persisted via a self-join on `plan(parent_plan_id)` and `proposed_action(parent_plan_id)`.

### Iterator — DepthFirstPlanIterator
- Pre-order DFS over an in-memory `PlanNode` tree, *no* JPA calls inside `next()`.
- **Why pure Java**: testable without a database, predictable performance (no N+1 surprises), and reusable. The Manager loads the subtree first (`eagerLoad`); the iterator just walks the loaded objects.
- Used by `PlanReportManager` for F10 and is the foundation for Week-2 dependency-graph topological checks.

### Template Method — AbstractLedgerEntryGenerator
- `generateEntries` is `final`; it locks down: select → validate → create transaction → build entries → post → afterPost.
- The conservation invariant (entries sum to zero) is enforced inside the `final` `postEntries` method. Subclasses cannot break double-entry accounting.
- The single subclass `ConsumableLedgerEntryGenerator` only overrides `selectAllocations` and `validate`.
- **Why this is the design that pre-empts Week-2 Change 2**: Week 2's `AssetLedgerEntryGenerator` is *one new subclass* — it overrides the same two abstract hooks plus `afterPost()` to write a utilisation record. Zero changes to the base class, no changes to the State machine, no changes to `LedgerManager` beyond a small selector that picks which generator to use per allocation kind.

### Visitor (Composite extension)
- `PlanNode#accept(PlanNodeVisitor)` is already in place. Week-2 metric visitors need only one new class each; existing `Plan` and `ProposedAction` `accept()` methods don't change.

### Posting Rule (eager)
- `PostingRuleEngine` is invoked by `LedgerManager` after each entry is applied to a pool account; if the balance dips below zero, an alert entry and an audit row are appended. The rule is keyed by account kind (`POOL` → alert), so additional rules in Week 2 (e.g. low-water threshold) plug in next to `onEntryPosted` without changing `LedgerManager` or any `Engine`.

## 5. How Week-2 changes hit minimal surface area

| Change                                  | Files touched                                 |
| --------------------------------------- | --------------------------------------------- |
| New action lifecycle state              | 1 new `*State` class + 1 line in registry + 1 transition edit |
| Asset-specific ledger generator         | 1 new subclass of `AbstractLedgerEntryGenerator`; small selector in `LedgerManager`  |
| New plan metric (e.g. cost roll-up)     | 1 new `PlanNodeVisitor` implementation        |
| New posting rule                        | 1 new method or strategy on `PostingRuleEngine` |
| New CRUD endpoint                       | 1 new controller method, 1 manager method     |

The patterns are not decorative: each one converts a class of Week-2 changes from a sweep across the codebase into an additive insert.
