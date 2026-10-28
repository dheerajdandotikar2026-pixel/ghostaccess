/*
 * GhostAccess is a deterministic, browser-only hackathon prototype.
 * All data below is synthetic. Simulation state is separate from source data.
 */

const findings = [
  {
    id: "finding_001",
    filter: ["critical", "stale"],
    initials: "AC",
    badge: "contractor",
    identity: "Analytics Contractor",
    identityShort: "Analytics Contractor",
    identityId: "contractor_101",
    identityType: "Contractor",
    target: "Customer Database",
    targetShort: "Customer Database",
    resourceType: "Critical resource",
    score: 90,
    severity: "CRITICAL",
    severityClass: "critical",
    status: "Terminated · 126d idle",
    summary: "A terminated contractor can reach critical customer data.",
    pathCount: 1,
    group: "Project Exporters",
    groupId: "group_exporters",
    role: "Customer Data Reader",
    roleId: "role_customer_reader",
    permission: "records:download",
    permissionId: "permission_download",
    nodeDetails: {
      identity: {
        type: "Identity", icon: "◉", className: "identity", title: "Analytics Contractor",
        description: "A former analytics project contractor whose account is still enabled.",
        facts: [["Status", "Terminated"], ["Last activity", "126 days ago"], ["Contract ended", "13 Jul 2026"], ["Account", "Enabled"]],
        note: "Flagged as stale — inactivity is evidence to review, not proof that access is unnecessary."
      },
      group: {
        type: "Group", icon: "⌘", className: "group", title: "Project Exporters",
        description: "A project group that grants export permissions to its current members.",
        facts: [["Members", "6 identities"], ["Owner", "Data Operations"], ["Purpose", "Analytics delivery"], ["Relation", "MEMBER_OF"]],
        note: "Removing this membership is a narrow, reversible candidate for human review."
      },
      role: {
        type: "Role", icon: "◇", className: "role", title: "Customer Data Reader",
        description: "A shared role that grants read and download operations on customer data.",
        facts: [["Privilege", "High"], ["Assignees", "4 active users"], ["Scope", "Customer data"], ["Relation", "ASSIGNED_ROLE"]],
        note: "Changing this shared role may affect legitimate active users and carries more collateral risk."
      },
      permission: {
        type: "Permission", icon: "↓", className: "permission", title: "records:download",
        description: "A high-severity permission that allows data exports from the customer environment.",
        facts: [["Severity", "90 / 100"], ["Action", "customer_records:download"], ["Granted by", "Customer Data Reader"], ["Relation", "GRANTS"]],
        note: "Export and download operations receive higher risk weight than simple read access."
      },
      resource: {
        type: "Resource", icon: "▦", className: "resource", title: "Customer Database",
        description: "A modeled database containing sensitive customer records.",
        facts: [["Sensitivity", "Critical · 100 / 100"], ["Owner", "Customer Platform"], ["Class", "Customer data"], ["Relation", "CAN_ACCESS"]],
        note: "The graph models synthetic relationships only; it does not evaluate a live policy or database."
      }
    },
    factors: [
      ["Resource sensitivity", "▦", "#ffe9ea", "#d7525a", 30, 100, "Critical customer records"],
      ["Permission severity", "↓", "#fff0dd", "#d98439", 25, 90, "Download capability"],
      ["Inactivity", "◷", "#eeeaff", "#715bc3", 20, 90, "126 days without activity"],
      ["Lifecycle risk", "!", "#ffece6", "#dd6544", 15, 100, "Contract marked terminated"],
      ["Credential risk", "⌁", "#e6f5f0", "#329873", 10, 45, "Linked key is stale"]
    ],
    candidates: [
      { id: "remove-membership", label: "Candidate", title: "Remove from Project Exporters", description: "Ends this identity’s path while preserving the group, role, and access of active users.", impact: "0 other identities", impactClass: "", breadth: 1, actions: [{ type: "REMOVE_EDGE", source: "contractor_101", target: "group_exporters", relation: "MEMBER_OF" }], visualEdges: [1], visualDisconnectedNodes: ["group", "role", "permission", "resource"] },
      { id: "narrow-role", label: "Candidate", title: "Remove download from shared role", description: "Closes the path, but also removes download access from active members of this role.", impact: "Shared role change", impactClass: "high", breadth: 3, actions: [{ type: "REMOVE_EDGE", source: "role_customer_reader", target: "permission_download", relation: "GRANTS" }], visualEdges: [3], visualDisconnectedNodes: ["permission", "resource"] },
      { id: "disable-identity", label: "Broad intervention", title: "Disable the identity", description: "Closes all modeled access for this account. Confirm no unrelated service dependency first.", impact: "Owner confirmation needed", impactClass: "unknown", breadth: 6, actions: [{ type: "DISABLE_IDENTITY", identity: "contractor_101" }], visualEdges: [1], visualDisconnectedNodes: ["identity", "group", "role", "permission", "resource"] }
    ]
  },
  {
    id: "finding_002", filter: ["stale"], initials: "RS", badge: "service", identity: "Reporting Service", identityShort: "Reporting Service", identityId: "service_204", identityType: "Service account", target: "Finance Warehouse", targetShort: "Finance Warehouse", resourceType: "Sensitive resource", score: 82, severity: "HIGH", severityClass: "high", status: "Owner set · 103d idle", summary: "An inactive service account retains broad finance export access.", pathCount: 1, group: "Finance Reports", groupId: "group_finance", role: "Finance Exporter", roleId: "role_finance_exporter", permission: "ledger:export", permissionId: "permission_export", nodeDetails: {}, factors: [], candidates: [
      { id: "remove-finance-membership", label: "Candidate", title: "Remove from Finance Reports", description: "Removes this dormant service account’s finance path while preserving access for current report services.", impact: "0 other identities", impactClass: "", breadth: 1, actions: [{ type: "REMOVE_EDGE", source: "service_204", target: "group_finance", relation: "MEMBER_OF" }], visualEdges: [1], visualDisconnectedNodes: ["group", "role", "permission", "resource"] },
      { id: "narrow-finance-role", label: "Candidate", title: "Remove export from shared role", description: "Closes this path, but changes a shared role used by active finance workflows.", impact: "Shared role change", impactClass: "high", breadth: 3, actions: [{ type: "REMOVE_EDGE", source: "role_finance_exporter", target: "permission_ledger_export", relation: "GRANTS" }], visualEdges: [3], visualDisconnectedNodes: ["permission", "resource"] },
      { id: "service-owner-review", label: "Broad intervention", title: "Disable the service account", description: "Removes all modeled paths. Ask the recorded owner to confirm scheduled jobs are no longer needed.", impact: "Owner confirmation needed", impactClass: "unknown", breadth: 6, actions: [{ type: "DISABLE_IDENTITY", identity: "service_204" }], visualEdges: [1], visualDisconnectedNodes: ["identity", "group", "role", "permission", "resource"] }
    ]
  },
  {
    id: "finding_003", filter: ["stale"], initials: "SK", badge: "key", identity: "Legacy Partner Key", identityShort: "Legacy Partner Key", identityId: "key_608", identityType: "API key", target: "Partner API", targetShort: "Partner API", resourceType: "External access", score: 76, severity: "HIGH", severityClass: "high", status: "Expired project · 184d unused", summary: "An old API key is tied to an expired partner project.", pathCount: 1, group: "Partner Integrations", groupId: "group_partner", role: "Partner API Admin", roleId: "role_partner_admin", permission: "partners:manage", permissionId: "permission_manage", nodeDetails: {}, factors: [], candidates: [
      { id: "deactivate-key", label: "Candidate", title: "Deactivate Legacy Partner Key", description: "Retires only the unused credential associated with the expired partner project.", impact: "0 other identities", impactClass: "", breadth: 1, actions: [{ type: "REMOVE_EDGE", source: "key_608", target: "group_partner", relation: "MEMBER_OF" }], visualEdges: [1], visualDisconnectedNodes: ["group", "role", "permission", "resource"] },
      { id: "remove-partner-group", label: "Candidate", title: "Remove Partner Integrations grant", description: "Closes this access route, but may affect other active partner integrations.", impact: "Shared group change", impactClass: "high", breadth: 3, actions: [{ type: "REMOVE_EDGE", source: "group_partner", target: "role_partner_admin", relation: "ASSIGNED_ROLE" }], visualEdges: [2], visualDisconnectedNodes: ["role", "permission", "resource"] },
      { id: "owner-review-key", label: "Review", title: "Confirm project closure", description: "Use when the project status or credential ownership needs a human decision before a change.", impact: "No graph change", impactClass: "unknown", breadth: 10, actions: [], visualEdges: [], visualDisconnectedNodes: [] }
    ]
  },
  {
    id: "finding_004", filter: ["critical", "stale"], initials: "ET", badge: "contractor", identity: "ETL Test Identity", identityShort: "ETL Test Identity", identityId: "test_811", identityType: "Test identity", target: "Customer Database", targetShort: "Customer Database", resourceType: "Critical resource", score: 69, severity: "MEDIUM", severityClass: "medium", status: "Inactive · 96d idle", summary: "A test identity has two modeled paths to customer records.", pathCount: 2, group: "QA Exporters", groupId: "group_qa", role: "Customer Data Reader", roleId: "role_customer_reader", permission: "records:download", permissionId: "permission_download", nodeDetails: {}, factors: [], candidates: [
      { id: "remove-qa-membership", label: "Candidate", title: "Remove from QA Exporters", description: "Removes one edge, but a second modeled path still reaches the customer database.", impact: "Alternative path remains", impactClass: "incomplete", breadth: 1, actions: [{ type: "REMOVE_EDGE", source: "test_811", target: "group_qa", relation: "MEMBER_OF" }], visualEdges: [1], visualDisconnectedNodes: [] },
      { id: "remove-both-test-groups", label: "Candidate", title: "Remove both test-group memberships", description: "Removes the two modeled routes from this inactive test identity without changing shared roles.", impact: "0 other identities", impactClass: "", breadth: 2, actions: [{ type: "REMOVE_EDGE", source: "test_811", target: "group_qa", relation: "MEMBER_OF" }, { type: "REMOVE_EDGE", source: "test_811", target: "group_test_ops", relation: "MEMBER_OF" }], visualEdges: [1], visualDisconnectedNodes: ["group", "role", "permission", "resource"] },
      { id: "disable-test-identity", label: "Broad intervention", title: "Disable ETL Test Identity", description: "Closes every modeled route for the inactive test account. Confirm its owner has retired dependent jobs.", impact: "Owner confirmation needed", impactClass: "unknown", breadth: 6, actions: [{ type: "DISABLE_IDENTITY", identity: "test_811" }], visualEdges: [1], visualDisconnectedNodes: ["identity", "group", "role", "permission", "resource"] }
    ]
  }
];

// This is the source graph. Simulations only ever receive a fresh copy of its edges.
const graphSource = (() => {
  const identities = [
    ["contractor_101", false], ["service_204", false], ["key_608", false], ["test_811", false],
    ["active_exporter_01", true], ["active_exporter_02", true], ["active_exporter_03", true],
    ["finance_reporter_01", true], ["finance_reporter_02", true], ["finance_reporter_03", true], ["finance_reporter_04", true],
    ["partner_integrator_01", true], ["partner_integrator_02", true], ["business_analyst_050", true],
    ["engineer_018", true], ["support_056", true], ["designer_020", true], ["product_007", true],
    ["owner_304", true], ["service_catalog_01", true], ["auditor_005", true], ["training_001", false],
    ["data_steward_01", true], ["qa_reviewer_02", true]
  ].map(([id, active]) => Object.freeze({ id, active }));

  const edges = [
    ["e01", "contractor_101", "group_exporters", "MEMBER_OF"], ["e02", "active_exporter_01", "group_exporters", "MEMBER_OF"], ["e03", "active_exporter_02", "group_exporters", "MEMBER_OF"], ["e04", "active_exporter_03", "group_exporters", "MEMBER_OF"],
    ["e05", "group_exporters", "role_customer_reader", "ASSIGNED_ROLE"], ["e06", "role_customer_reader", "permission_download", "GRANTS"], ["e07", "permission_download", "customer_database", "CAN_ACCESS"],
    ["e08", "service_204", "group_finance", "MEMBER_OF"], ["e09", "finance_reporter_01", "group_finance", "MEMBER_OF"], ["e10", "finance_reporter_02", "group_finance", "MEMBER_OF"], ["e11", "finance_reporter_03", "group_finance", "MEMBER_OF"], ["e12", "finance_reporter_04", "group_finance", "MEMBER_OF"],
    ["e13", "group_finance", "role_finance_exporter", "ASSIGNED_ROLE"], ["e14", "role_finance_exporter", "permission_ledger_export", "GRANTS"], ["e15", "permission_ledger_export", "finance_warehouse", "CAN_ACCESS"],
    ["e16", "key_608", "group_partner", "MEMBER_OF"], ["e17", "partner_integrator_01", "group_partner", "MEMBER_OF"], ["e18", "partner_integrator_02", "group_partner", "MEMBER_OF"], ["e19", "group_partner", "role_partner_admin", "ASSIGNED_ROLE"], ["e20", "role_partner_admin", "permission_partner_manage", "GRANTS"], ["e21", "permission_partner_manage", "partner_api", "CAN_ACCESS"],
    ["e22", "test_811", "group_qa", "MEMBER_OF"], ["e23", "qa_reviewer_02", "group_qa", "MEMBER_OF"], ["e24", "group_qa", "role_customer_reader", "ASSIGNED_ROLE"],
    ["e25", "test_811", "group_test_ops", "MEMBER_OF"], ["e26", "group_test_ops", "role_backup_reader", "ASSIGNED_ROLE"], ["e27", "role_backup_reader", "permission_backup_read", "GRANTS"], ["e28", "permission_backup_read", "customer_database", "CAN_ACCESS"],
    ["e29", "training_001", "group_training", "MEMBER_OF"], ["e30", "group_training", "role_docs_reader", "ASSIGNED_ROLE"], ["e31", "role_docs_reader", "permission_docs_read", "GRANTS"], ["e32", "permission_docs_read", "training_docs", "CAN_ACCESS"]
  ].map(([id, source, target, relation]) => Object.freeze({ id, source, target, relation }));

  return Object.freeze({ identities: Object.freeze(identities), edges: Object.freeze(edges) });
})();

const graphTargets = Object.freeze({
  finding_001: "customer_database",
  finding_002: "finance_warehouse",
  finding_003: "partner_api",
  finding_004: "customer_database"
});
const sourceEdgeSignature = graphSource.edges.map((edge) => `${edge.id}:${edge.source}:${edge.target}:${edge.relation}`).join("|");

const baselineVerifiedSimulations = 12;
const storageKey = "ghostaccess-verified-simulations";
const savedVerifiedSimulations = (() => {
  try { return new Set(JSON.parse(localStorage.getItem(storageKey) || "[]")); } catch { return new Set(); }
})();
const state = { selectedFindingId: "finding_001", selectedNode: "identity", selectedCandidateId: "remove-membership", simulated: false, simulation: null, graphZoom: 1, filter: "all", shownAll: false, verifiedSimulations: savedVerifiedSimulations };

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => [...document.querySelectorAll(selector)];
const selectedFinding = () => findings.find((finding) => finding.id === state.selectedFindingId) || findings[0];

function cloneGraph() {
  return { edges: graphSource.edges.map((edge) => ({ ...edge })) };
}

function findPaths(graph, source, target, limit = 8) {
  const adjacency = new Map();
  graph.edges.forEach((edge) => {
    if (!adjacency.has(edge.source)) adjacency.set(edge.source, []);
    adjacency.get(edge.source).push(edge.target);
  });

  const paths = [];
  const queue = [[source]];
  while (queue.length && paths.length < limit) {
    const path = queue.shift();
    const current = path[path.length - 1];
    if (current === target) {
      paths.push(path);
      continue;
    }
    if (path.length > 12) continue;
    (adjacency.get(current) || []).forEach((next) => {
      if (!path.includes(next)) queue.push([...path, next]);
    });
  }
  return paths;
}

function applyAction(graph, action) {
  if (action.type === "REMOVE_EDGE") {
    graph.edges = graph.edges.filter((edge) => !(edge.source === action.source && edge.target === action.target && (!action.relation || edge.relation === action.relation)));
  }
  if (action.type === "DISABLE_IDENTITY") {
    graph.edges = graph.edges.filter((edge) => edge.source !== action.identity);
  }
}

function evaluateCandidate(finding, candidate) {
  const target = graphTargets[finding.id];
  const sourceGraph = { edges: graphSource.edges };
  const beforePaths = findPaths(sourceGraph, finding.identityId, target);
  const graphCopy = cloneGraph();
  (candidate.actions || []).forEach((action) => applyAction(graphCopy, action));
  const afterPaths = findPaths(graphCopy, finding.identityId, target);
  const collateralImpacts = graphSource.identities
    .filter((identity) => identity.active && identity.id !== finding.identityId)
    .filter((identity) => findPaths(sourceGraph, identity.id, target).length > 0 && findPaths(graphCopy, identity.id, target).length === 0)
    .map((identity) => identity.id);
  const sourceIntact = graphSource.edges.map((edge) => `${edge.id}:${edge.source}:${edge.target}:${edge.relation}`).join("|") === sourceEdgeSignature;
  const success = beforePaths.length > 0 && afterPaths.length === 0;
  return {
    beforePaths,
    afterPaths,
    beforeReachable: beforePaths.length > 0,
    afterReachable: afterPaths.length > 0,
    collateralImpacts,
    sourceIntact,
    success,
    cost: success ? collateralImpacts.length * 100 + (candidate.breadth || (candidate.actions || []).length) : Number.POSITIVE_INFINITY
  };
}

function recommendedCandidate(finding) {
  return finding.candidates
    .map((candidate) => ({ candidate, report: evaluateCandidate(finding, candidate) }))
    .filter(({ report }) => report.success)
    .sort((left, right) => left.report.cost - right.report.cost)[0]?.candidate || finding.candidates[0];
}

function renderFindings() {
  const list = $("#findingList");
  const filtered = findings.filter((finding) => state.filter === "all" || finding.filter.includes(state.filter));
  const visible = state.shownAll ? filtered : filtered.slice(0, 3);
  list.innerHTML = visible.map((finding) => `
    <button class="finding-row ${finding.id === state.selectedFindingId ? "selected" : ""}" data-finding-id="${finding.id}" role="listitem" aria-label="Open ${finding.identity} finding">
      <span class="finding-person"><span class="person-badge ${finding.badge}">${finding.initials}</span><span><b class="finding-name">${finding.identity}</b><small class="finding-sub">${finding.status}</small></span></span>
      <span><b class="finding-target">${finding.target}</b><small class="finding-target-sub">${finding.resourceType}</small></span>
      <span class="score-pill ${finding.severityClass}">${finding.score}</span>
      <span class="severity ${finding.severityClass}">${finding.severity}</span>
      <span class="row-arrow">›</span>
    </button>`).join("");
  $$(".finding-row").forEach((row) => row.addEventListener("click", () => selectFinding(row.dataset.findingId)));
}

function renderMetrics() {
  $("#identityCount").textContent = graphSource.identities.length;
  $("#riskFindingCount").textContent = findings.filter((finding) => finding.score >= 65).length;
  $("#credentialCount").textContent = "7";
  $("#simulationCount").textContent = baselineVerifiedSimulations + state.verifiedSimulations.size;
}

function runAnalysis() {
  const results = findings.map((finding) => ({
    finding,
    beforePaths: findPaths({ edges: graphSource.edges }, finding.identityId, graphTargets[finding.id]),
    recommended: recommendedCandidate(finding)
  }));
  const allFindingsReachable = results.every(({ beforePaths }) => beforePaths.length > 0);
  const allRecommendationsVerified = results.every(({ finding, recommended }) => evaluateCandidate(finding, recommended).success);
  renderMetrics();
  $("#analysisTimestamp").textContent = `Analysis refreshed just now · ${results.length} findings evaluated`;
  return { allFindingsReachable, allRecommendationsVerified, count: results.length };
}

function selectFinding(id) {
  const finding = findings.find((item) => item.id === id);
  if (!finding) return;
  state.selectedFindingId = id;
  state.selectedNode = "identity";
  state.simulated = false;
  state.simulation = null;
  state.selectedCandidateId = recommendedCandidate(finding).id;
  renderAll();
  showToast(`${finding.identity} opened for investigation`);
}

function renderRiskCard() {
  const finding = selectedFinding();
  $("#scoreRing").style.setProperty("--score", finding.score);
  $("#scoreNumber").textContent = finding.score;
  $("#riskTitle");
  $("#riskSummary").textContent = finding.summary;
  $("#riskPathCount").textContent = `${finding.pathCount} exposed path${finding.pathCount > 1 ? "s" : ""}`;
  const severity = finding.score >= 85 ? "High exposure" : finding.score >= 75 ? "Elevated exposure" : "Needs review";
  $("#risk-title").textContent = severity;
  const indicator = $("#scaleIndicator");
  indicator.style.right = "auto";
  indicator.style.left = `${Math.max(3, Math.min(93, finding.score - 3))}%`;
}

function field(title, value) { return `<div><dt>${title}</dt><dd>${value}</dd></div>`; }
function genericNodeDetails(finding, node) {
  const fallback = {
    identity: { type: finding.identityType, icon: "◉", className: "identity", title: finding.identity, description: `A synthetic ${finding.identityType.toLowerCase()} that was flagged during deterministic analysis.`, facts: [["Status", finding.status], ["Identity ID", finding.identityId], ["Risk score", `${finding.score} / 100`], ["Target", finding.target]], note: "This finding is based only on the modeled synthetic graph and lifecycle signals." },
    group: { type: "Group", icon: "⌘", className: "group", title: finding.group, description: "A modeled group on the selected permission path.", facts: [["Group ID", finding.groupId], ["Relation", "MEMBER_OF"], ["Purpose", "Synthetic demo"], ["Path target", finding.target]], note: "Group membership is evaluated as a candidate change on a graph copy." },
    role: { type: "Role", icon: "◇", className: "role", title: finding.role, description: "A modeled role on the selected permission path.", facts: [["Role ID", finding.roleId], ["Relation", "ASSIGNED_ROLE"], ["Privilege", "High"], ["Path target", finding.target]], note: "Shared roles may have impact beyond this identity; review ownership before a change." },
    permission: { type: "Permission", icon: "↓", className: "permission", title: finding.permission, description: "A modeled permission involved in effective access.", facts: [["Permission ID", finding.permissionId], ["Relation", "GRANTS"], ["Severity", "High"], ["Target", finding.target]], note: "Permission severity is one weighted factor, not a production policy decision." },
    resource: { type: "Resource", icon: "▦", className: "resource", title: finding.target, description: "A synthetic sensitive resource reached by the selected path.", facts: [["Resource", finding.target], ["Class", finding.resourceType], ["Sensitivity", "High"], ["Relation", "CAN_ACCESS"]], note: "Reachability is verified only within this modeled, synthetic graph." }
  };
  return fallback[node];
}

function renderGraph() {
  const finding = selectedFinding();
  $("#graphCanvas").style.setProperty("--graph-zoom", state.graphZoom);
  $("#graphIdentity").textContent = finding.identityShort;
  $("#graphIdentityMeta").textContent = finding.status;
  $("#graphGroup").textContent = finding.group;
  $("#graphGroupMeta").textContent = "Group";
  $("#graphRole").textContent = finding.role;
  $("#graphRoleMeta").textContent = "Role";
  $("#graphPermission").textContent = finding.permission;
  $("#graphPermissionMeta").textContent = "Permission · High";
  $("#graphResource").textContent = finding.targetShort;
  $("#graphResourceMeta").textContent = finding.resourceType;
  const candidate = currentCandidate();
  const result = state.simulation;
  const pathBlocked = Boolean(result?.success);
  const removedEdges = state.simulated ? candidate.visualEdges || [] : [];
  [1, 2, 3, 4].forEach((index) => {
    const edge = $(`#edge${index}`);
    edge.classList.toggle("removed-edge", removedEdges.includes(index));
    edge.classList.toggle("active-edge", !removedEdges.includes(index));
  });
  $("#pathStatus").textContent = pathBlocked ? "Path removed in simulation" : state.simulated ? "Alternative modeled path remains" : "Path currently grants access";
  $(".pulse-dot").style.background = pathBlocked ? "#31a673" : state.simulated ? "#e09b4a" : "#e7595e";
  $("#graphCaption").textContent = finding.pathCount > 1
    ? "Two modeled paths exist. This view shows one; simulation evaluates both."
    : "Each link represents modeled access, not a live IAM decision.";
  $$(".graph-node").forEach((node) => {
    node.classList.toggle("selected", node.dataset.node === state.selectedNode);
    node.classList.toggle("disconnected", pathBlocked && (candidate.visualDisconnectedNodes || []).includes(node.dataset.node));
  });
}

function renderNodeDetails() {
  const finding = selectedFinding();
  const detail = finding.nodeDetails?.[state.selectedNode] || genericNodeDetails(finding, state.selectedNode);
  $("#detailType").innerHTML = `<span class="type-icon ${detail.className}">${detail.icon}</span> ${detail.type}`;
  $("#detailTitle").textContent = detail.title;
  $("#detailDescription").textContent = detail.description;
  $("#detailFacts").innerHTML = detail.facts.map(([label, value]) => field(label, value)).join("");
  $("#detailNote").innerHTML = `<span>i</span><p>${detail.note}</p>`;
}

function renderReasons() {
  const finding = selectedFinding();
  const factors = finding.factors?.length ? finding.factors : [
    ["Resource sensitivity", "▦", "#ffe9ea", "#d7525a", 30, 90, finding.resourceType],
    ["Permission severity", "↓", "#fff0dd", "#d98439", 25, 82, "Elevated access"],
    ["Inactivity", "◷", "#eeeaff", "#715bc3", 20, 86, finding.status],
    ["Lifecycle risk", "!", "#ffece6", "#dd6544", 15, 75, "Lifecycle signal present"],
    ["Credential risk", "⌁", "#e6f5f0", "#329873", 10, 65, "Credential review needed"]
  ];
  $("#reasonGrid").innerHTML = factors.map(([title, icon, background, color, weight, score, reason]) => `
    <article class="reason-card">
      <div class="reason-top"><span class="reason-icon" style="background:${background};color:${color}">${icon}</span><span class="reason-weight">${weight}% weight</span></div>
      <h3>${title}</h3><p>${reason}</p><div class="reason-progress"><span style="width:${score}%;background:${color}"></span></div>
    </article>`).join("");
}

function currentCandidate() {
  const finding = selectedFinding();
  return finding.candidates?.find((candidate) => candidate.id === state.selectedCandidateId) || finding.candidates?.[0];
}

function selectCandidate(candidateId, moveFocus = false) {
  state.selectedCandidateId = candidateId;
  state.simulated = false;
  state.simulation = null;
  renderRecommendations(); renderSimulationResult(); renderGraph();
  if (moveFocus) requestAnimationFrame(() => $(`[data-candidate-id="${candidateId}"]`)?.focus());
}

function renderRecommendations() {
  const finding = selectedFinding();
  const candidates = finding.candidates || [];
  const reports = candidates.map((candidate) => ({ candidate, result: evaluateCandidate(finding, candidate) }));
  const preferred = reports.filter(({ result }) => result.success).sort((left, right) => left.result.cost - right.result.cost)[0]?.candidate;
  $("#recommendations").innerHTML = reports.map(({ candidate, result }) => {
    const recommended = candidate.id === preferred?.id;
    const status = !result.success ? "Leaves path intact" : recommended ? "Lowest impact" : "Viable option";
    const impact = result.collateralImpacts.length
      ? `${result.collateralImpacts.length} active ${result.collateralImpacts.length === 1 ? "identity" : "identities"} affected`
      : candidate.impact;
    const impactClass = !result.success ? "incomplete" : candidate.impactClass || "";
    const label = !result.success ? "Rejected" : recommended ? "Recommended" : candidate.label;
    return `
    <button class="recommendation ${candidate.id === state.selectedCandidateId ? "selected" : ""}" data-candidate-id="${candidate.id}" role="radio" aria-checked="${candidate.id === state.selectedCandidateId}" tabindex="${candidate.id === state.selectedCandidateId ? "0" : "-1"}">
      <span class="candidate-label">${label}</span>
      <h3>${candidate.title}</h3>
      <p>${candidate.description}</p>
      <span class="candidate-meta"><span class="${recommended ? "recommended-tag" : ""}">${status}</span><span class="impact-tag ${impactClass}">${impact}</span></span>
    </button>`;
  }).join("");
  const recommendationButtons = $$(".recommendation");
  recommendationButtons.forEach((button, index) => {
    button.addEventListener("click", () => selectCandidate(button.dataset.candidateId));
    button.addEventListener("keydown", (event) => {
      const navigation = { ArrowRight: 1, ArrowDown: 1, ArrowLeft: -1, ArrowUp: -1 };
      if (!(event.key in navigation) && event.key !== "Home" && event.key !== "End") return;
      event.preventDefault();
      const nextIndex = event.key === "Home" ? 0 : event.key === "End" ? recommendationButtons.length - 1 : (index + navigation[event.key] + recommendationButtons.length) % recommendationButtons.length;
      selectCandidate(recommendationButtons[nextIndex].dataset.candidateId, true);
    });
  });
}

function renderSimulationResult() {
  const simulation = state.simulation;
  const after = simulation ? simulation.afterReachable ? "Still reachable" : "Not reachable" : "Not yet run";
  const collateral = simulation?.collateralImpacts.length || 0;
  const description = !simulation
    ? "Choose a change to verify it"
    : simulation.success
      ? `No modeled path remains · ${collateral ? `${collateral} active ${collateral === 1 ? "identity" : "identities"} would lose this target` : "no other active identity loses this target"}`
      : `Candidate rejected · ${simulation.afterPaths.length} modeled path${simulation.afterPaths.length === 1 ? " remains" : "s remain"}`;
  $("#beforeState").textContent = "Reachable";
  $("#afterState").textContent = after;
  $("#afterState").classList.toggle("verified", Boolean(simulation?.success));
  $("#afterDescription").textContent = description;
  $("#simulationDisclaimer").textContent = simulation
    ? simulation.success
      ? "Verified against a graph copy. Source graph unchanged."
      : "Evaluated against a graph copy. Source graph unchanged."
    : "Source graph remains unchanged.";
  $("#simulateButton").innerHTML = simulation ? "Run a new simulation <span>↻</span>" : "Simulate selected change <span>→</span>";
}

function renderAll() { renderMetrics(); renderFindings(); renderRiskCard(); renderGraph(); renderNodeDetails(); renderReasons(); renderRecommendations(); renderSimulationResult(); }

function showToast(message) {
  const toast = $("#toast");
  toast.textContent = message;
  toast.classList.add("show");
  clearTimeout(showToast.timeout);
  showToast.timeout = setTimeout(() => toast.classList.remove("show"), 2800);
}

function openDialog(title, content) {
  $("#dialogTitle").textContent = title;
  $("#dialogContent").textContent = content;
  $("#infoDialog").showModal();
}

function installInteractions() {
  $$(".graph-node").forEach((node) => node.addEventListener("click", () => { state.selectedNode = node.dataset.node; renderGraph(); renderNodeDetails(); }));
  $$(".zoom-button").forEach((button) => button.addEventListener("click", () => {
    if (button.dataset.zoom === "fit") state.graphZoom = 1;
    if (button.dataset.zoom === "in") state.graphZoom = Math.min(1.05, Number((state.graphZoom + 0.05).toFixed(2)));
    if (button.dataset.zoom === "out") state.graphZoom = Math.max(0.85, Number((state.graphZoom - 0.05).toFixed(2)));
    renderGraph();
  }));
  $("#simulateButton").addEventListener("click", () => {
    const candidate = currentCandidate();
    const simulation = evaluateCandidate(selectedFinding(), candidate);
    state.simulated = true;
    state.simulation = simulation;
    if (simulation.success) {
      state.verifiedSimulations.add(`${selectedFinding().id}:${candidate.id}`);
      try { localStorage.setItem(storageKey, JSON.stringify([...state.verifiedSimulations])); } catch { /* Private browsing can disable local storage. */ }
    }
    renderMetrics(); renderSimulationResult(); renderGraph();
    showToast(simulation.success
      ? "Simulation verified: no modeled path remains."
      : `Simulation rejected: ${simulation.afterPaths.length} modeled path${simulation.afterPaths.length === 1 ? " remains" : "s remain"}.`);
  });
  $("#startInvestigation").addEventListener("click", () => { document.querySelector("#investigate").scrollIntoView({ behavior: "smooth", block: "start" }); showToast("Top finding ready for investigation"); });
  $("#riskDetailsButton").addEventListener("click", () => document.querySelector(".investigation-section").scrollIntoView({ behavior: "smooth", block: "start" }));
  $("#graphResetButton").addEventListener("click", () => { state.simulated = false; state.simulation = null; state.graphZoom = 1; state.selectedNode = "identity"; renderGraph(); renderNodeDetails(); renderSimulationResult(); showToast("Graph reset to source state"); });
  $("#filterButton").addEventListener("click", () => { const menu = $("#filterMenu"); menu.hidden = !menu.hidden; $("#filterButton").setAttribute("aria-expanded", String(!menu.hidden)); });
  $$("#filterMenu button").forEach((button) => button.addEventListener("click", () => { state.filter = button.dataset.filter; $$("#filterMenu button").forEach((item) => item.classList.toggle("selected", item === button)); $("#filterMenu").hidden = true; $("#filterButton").setAttribute("aria-expanded", "false"); $("#filterButton").innerHTML = `${button.textContent} <span>⌄</span>`; renderFindings(); }));
  $("#showAllButton").addEventListener("click", () => { state.shownAll = !state.shownAll; $("#showAllButton").innerHTML = state.shownAll ? "Show prioritized findings <span>→</span>" : "View all findings <span>→</span>"; renderFindings(); });
  $("#refreshButton").addEventListener("click", (event) => {
    const refreshControl = event.currentTarget;
    refreshControl.classList.add("refreshing");
    const analysis = runAnalysis();
    setTimeout(() => refreshControl.classList.remove("refreshing"), 650);
    showToast(analysis.allFindingsReachable && analysis.allRecommendationsVerified
      ? `Analysis complete: ${analysis.count} findings and recommendations verified.`
      : "Analysis complete: a finding requires review.");
  });
  $("#helpButton").addEventListener("click", () => openDialog("Designed for a safer demo.", "GhostAccess is a deterministic synthetic-data prototype. It highlights modeled access paths and evaluates recommendations on a graph copy. It never reads or changes a real cloud account."));
  $("#scopeButton").addEventListener("click", () => openDialog("Prototype scope.", "This hackathon build models identities, groups, roles, permissions, and resources from local synthetic data. It does not provide a production IAM policy evaluator, automated revocation, cloud integration, or approval workflow."));
  $("#dialogClose").addEventListener("click", () => $("#infoDialog").close());
  $("#dialogAction").addEventListener("click", () => $("#infoDialog").close());
  $("#menuButton").addEventListener("click", () => {
    const menu = $("#mobileMenu");
    menu.hidden = !menu.hidden;
    $("#menuButton").setAttribute("aria-expanded", String(!menu.hidden));
  });
  $$("#mobileMenu a").forEach((link) => link.addEventListener("click", () => {
    $("#mobileMenu").hidden = true;
    $("#menuButton").setAttribute("aria-expanded", "false");
  }));
  $$(".nav-link").forEach((link) => link.addEventListener("click", () => { $$(".nav-link").forEach((item) => item.classList.toggle("active", item === link)); }));
}

renderAll();
installInteractions();
