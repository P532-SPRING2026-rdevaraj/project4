---
title: "Resource Planning Ledger -- Design Document (Week 1)"
author: "Rohith Gowda Devaraju · CSCI-P532, Spring 2026"
geometry: "margin=0.4in"
fontsize: 9.5pt
mainfont: "Helvetica"
monofont: "Menlo"
header-includes:
  - \usepackage{setspace}
  - \setstretch{1.0}
  - \usepackage{enumitem}
  - \setlist{nosep,leftmargin=*}
  - \pagenumbering{gobble}
  - \usepackage{titlesec}
  - \titlespacing*{\section}{0pt}{6pt}{2pt}
  - \titlespacing*{\subsection}{0pt}{4pt}{2pt}
  - \setlength{\parskip}{2pt}
  - \setlength{\parindent}{0pt}
---

## 1. Four-layer architecture

| Layer | Stereotype | Responsibility |
|---|---|---|
| **Client** | `@RestController` | HTTP routing only; zero business logic |
| **Manager** | `@Service` | Orchestrates use-case sequences |
| **Engine** | `@Service` / POJO | One replaceable algorithm per class |
| **ResourceAccess** | `@Repository` | Atomic JPA verbs; no SQL in callers |

Invariants: controllers never contain business logic; engines never call other engines; repositories never publish events.

## 2. UML class diagram (four patterns + knowledge/operational split)

\scriptsize
```
                                  PlanNodeVisitor (interface)        <-- Visitor (Composite extension)
                                          ^
              +----------------------+    |
              | PlanNode (interface) |<---+               <-- Composite
              |  +getStatus()        |
              |  +getTotalAlloc(rt)  |
              |  +accept(visitor)    |
              +----+------------+----+
                   |            |
        +----------+            +-----------+
        v                                   v
 +----------------+ allocs *      +--------------------+ children: List<PlanNode>
 | ProposedAction |-------------->| Plan (COMPOSITE)   | (leaf+composite mixed)
 | (LEAF)         |               | derived getStatus()|
 | status: enum   |               +--------------------+
 +-------+--------+                                              KNOWLEDGE LEVEL
         | uses                                                  - Protocol
         v                                                       - ProtocolStep
 +----------------------+    one stateless @Component bean       - ResourceType
 | ActionContext        |    per state, resolved by              (rarely changed,
 |  -action             |    ActionStateRegistry from the         never mutated by
 |  -callbacks ---------+--> ActionStateCallbacks (ActionManager) execution)
 +-----+----------------+
       |                                                         OPERATIONAL LEVEL
       v                                                         - Plan, Action,
 +----------------------+   <-- State pattern                      ImplementedAction,
 | ActionState (iface)  |   Proposed | Suspended | InProgress |    Suspension,
 |  +implement(ctx)     |   Completed | Abandoned                  Allocation,
 |  +suspend(ctx,r)     |   illegal transitions throw              Transaction,
 |  +resume / +complete |   IllegalStateTransitionException        Entry, AuditLog
 |  +abandon(ctx)       |
 +----------------------+

 +---------------------+  +-----------------------+
 | Iterator<PlanNode>  |<-| DepthFirstPlanIterator|  <-- Iterator (pure Java; no JPA in
 +---------------------+  +-----------------------+      next(); Manager loads tree first)

 +----------------------------------+
 | AbstractLedgerEntryGenerator     |  <-- Template Method skeleton:
 |  +generateEntries(impl)   FINAL  |  select -> validate -> createTx -> buildW/D
 |  #selectAllocations(...)  ABSTRACT  -> postEntries (FINAL: conservation sum=0
 |  #validate(...)           ABSTRACT     check lives here) -> afterPost
 |  #buildWithdrawal/Deposit         |
 |  #afterPost(tx)           HOOK   |  <-- Week-2 extension point (empty in Week 1)
 |  -postEntries(...)        FINAL  |
 +----------------^-----------------+
                  |
 +----------------+--------------+      +-----------------------------+
 | ConsumableLedgerEntryGenerator|      | PostingRuleEngine (eager)   |
 | filters CONSUMABLE allocs;    |      | pool balance < 0 ->         |
 | validates positive quantity   |      | append alert + audit row    |
 +-------------------------------+      +-----------------------------+
```
\normalsize

\clearpage

\raggedright

## 3. Pattern justification

State. Every ActionState is a stateless Spring Component singleton, and ActionContext carries the mutable action together with a callback surface. Side effects such as creating an ImplementedAction, posting ledger entries, and opening or closing a Suspension flow through ActionStateCallbacks, so states never depend on the Manager or on repositories. Choosing State over a switch means a Week-2 lifecycle change, for example a PARTIALLY_COMPLETED state, requires only one new class, one map entry in the registry, and one outgoing transition edit. A switch would explode combinatorially across the five existing operations.

Composite. Plan and ProposedAction both implement PlanNode. The status on a Plan is derived recursively per the spec: COMPLETED only if all children are completed; IN_PROGRESS if any child is in progress or completed but not all; SUSPENDED if any child is suspended and none is in progress; ABANDONED if all children are abandoned. The total allocated quantity is summed recursively over leaf descendants. Choosing Composite is what allows the F10 plan-summary report and the Week-2 dependency-graph traversal to walk leaves and sub-plans uniformly; without it, every traversal would need explicit type checks. The composite tree is persisted with self-joins on the parent plan column per the design hint.

Iterator. The DepthFirstPlanIterator performs a pre-order depth-first walk over an in-memory PlanNode tree, with no JPA calls inside the next method. The PlanManager fetches the subtree first and then hands it to the iterator. Keeping the iterator in pure Java means it is fully testable without a database, avoids N+1 query surprises in production, and is reused unchanged by the Week-2 dependency-check that needs leaves in topological order.

Template Method. The generateEntries method is final and locks down the skeleton, and the double-entry conservation check that requires entries to sum to zero lives inside the final postEntries method, so subclasses cannot break the ledger's accounting invariant. The Week-1 ConsumableLedgerEntryGenerator only overrides selectAllocations and validate. This is the design that pre-empts the Week-2 asset-ledger change: the asset generator becomes a single new subclass overriding the same two abstract hooks plus the previously empty afterPost hook to write a utilisation record. The base class never changes, the State machine never changes, and only a small selector is added in LedgerManager to choose between the two generators.

Visitor (Composite extension). The accept method on PlanNode that takes a PlanNodeVisitor is already wired in. The three Week-2 metric visitors covering cost, time, and load require zero edits to existing node classes; only one new visitor implementation per metric needs to be written.

## 4. Knowledge vs. operational split

A Plan copies information from a Protocol at creation time; nothing in plan execution writes back into a Protocol row. Knowledge-level entities, namely Protocol, ProtocolStep, and ResourceType, are owned by ProtocolManager and ResourceTypeManager. Operational entities, namely Plan, ProposedAction, ImplementedAction, Suspension, ResourceAllocation, Transaction, Entry, and AuditLogEntry, are owned by PlanManager, ActionManager, LedgerManager, and AuditManager. Because these two groups live in separate Manager and Repository classes, Week-2 operational changes never touch knowledge-level code.
