const state = {
    brand: { appName: "OVS", organizationName: "Your Organization", accentColor: "#0f766e" },
    overview: {},
    election: null,
    candidates: [],
    voters: [],
    services: [],
    results: null,
    user: readSession()
};

const electionId = "ovs-board-2026";

const els = {
    brandName: document.getElementById("brand-name"),
    orgName: document.getElementById("org-name"),
    sessionChip: document.getElementById("session-chip"),
    authAction: document.getElementById("auth-action"),
    loginForm: document.getElementById("login-form"),
    loginEmail: document.getElementById("login-email"),
    loginPassword: document.getElementById("login-password"),
    loginMessage: document.getElementById("login-message"),
    overviewMetrics: document.getElementById("overview-metrics"),
    consoleStatus: document.getElementById("console-status"),
    consoleElection: document.getElementById("console-election"),
    turnoutLabel: document.getElementById("turnout-label"),
    turnoutBar: document.getElementById("turnout-bar"),
    ballotElection: document.getElementById("ballot-election"),
    ballotStatus: document.getElementById("ballot-status"),
    candidateList: document.getElementById("candidate-list"),
    candidateSelect: document.getElementById("candidate-select"),
    voteForm: document.getElementById("vote-form"),
    voteEmail: document.getElementById("vote-email"),
    voteMessage: document.getElementById("vote-message"),
    voterForm: document.getElementById("voter-form"),
    voterName: document.getElementById("voter-name"),
    voterEmail: document.getElementById("voter-email"),
    voterEligible: document.getElementById("voter-eligible"),
    voterTable: document.getElementById("voter-table"),
    serviceList: document.getElementById("service-list"),
    trustList: document.getElementById("trust-list"),
    publishResults: document.getElementById("publish-results"),
    refreshResults: document.getElementById("refresh-results"),
    resultElection: document.getElementById("result-election"),
    resultPublication: document.getElementById("result-publication"),
    resultBars: document.getElementById("result-bars"),
    resultSummary: document.getElementById("result-summary")
};

function readSession() {
    try {
        return JSON.parse(localStorage.getItem("ovs-session")) || null;
    } catch {
        return null;
    }
}

function writeSession(user) {
    state.user = user;
    if (user) {
        localStorage.setItem("ovs-session", JSON.stringify(user));
    } else {
        localStorage.removeItem("ovs-session");
    }
    renderSession();
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

async function request(url, options = {}) {
    const response = await fetch(url, {
        headers: { "Content-Type": "application/json" },
        ...options
    });
    const payload = await response.json().catch(() => ({
        success: false,
        message: "Server returned an unreadable response."
    }));
    if (!response.ok || payload.success === false) {
        throw new Error(payload.message || "Request failed.");
    }
    return payload.data;
}

function metric(label, value) {
    return `<div><dt>${escapeHtml(label)}</dt><dd>${escapeHtml(value)}</dd></div>`;
}

function percent(value) {
    const number = Number(value || 0);
    return Math.max(0, Math.min(100, number));
}

function route() {
    const current = (location.hash || "#landing").replace("#", "");
    const allowed = ["landing", "login", "vote", "admin", "results"];
    const next = allowed.includes(current) ? current : "landing";

    document.querySelectorAll(".view").forEach((view) => {
        view.classList.toggle("active", view.dataset.view === next);
    });
    document.querySelectorAll("[data-nav]").forEach((link) => {
        link.classList.toggle("active", link.dataset.nav === next);
    });

    if (next === "vote" && !state.user) {
        location.hash = "login";
        setMessage(els.loginMessage, "Please sign in before opening the voter dashboard.");
    }
    if (next === "admin" && state.user?.role !== "ORG_ADMIN") {
        location.hash = "login";
        setMessage(els.loginMessage, "Admin access is required for registry and publication controls.");
    }
}

function setMessage(target, message, kind = "neutral") {
    target.textContent = message;
    target.dataset.kind = kind;
}

function renderBrand() {
    document.title = `${state.brand.appName} - ${state.brand.organizationName}`;
    document.documentElement.style.setProperty("--accent", state.brand.accentColor);
    els.brandName.textContent = state.brand.appName;
    els.orgName.textContent = state.brand.organizationName;
}

function renderSession() {
    if (state.user) {
        els.sessionChip.textContent = `${state.user.name} · ${state.user.role}`;
        els.authAction.textContent = "Sign Out";
        if (state.user.email && !els.voteEmail.value) {
            els.voteEmail.value = state.user.email;
        }
    } else {
        els.sessionChip.textContent = "Guest";
        els.authAction.textContent = "Sign In";
    }
}

function renderOverview() {
    const data = state.overview;
    const turnout = percent(data.turnoutPercent);
    els.consoleElection.textContent = data.activeElection || "No active election";
    els.consoleStatus.textContent = data.status || "UNKNOWN";
    els.turnoutLabel.textContent = `${turnout}% turnout`;
    els.turnoutBar.style.width = `${turnout}%`;
    els.overviewMetrics.innerHTML = [
        metric("Eligible voters", data.eligibleVoters ?? 0),
        metric("Votes cast", data.votesCast ?? 0),
        metric("Turnout", `${turnout}%`),
        metric("Status", data.status || "Unknown")
    ].join("");
}

function renderElection() {
    if (!state.election) {
        els.ballotElection.textContent = "No election available";
        els.candidateList.innerHTML = `<div class="empty-state">No election has been configured yet.</div>`;
        return;
    }

    els.ballotElection.textContent = state.election.name;
    els.ballotStatus.textContent = state.election.status;
    els.candidateList.innerHTML = state.candidates.map((candidate) => `
        <article class="candidate-card">
            <span class="badge ok">${escapeHtml(candidate.id)}</span>
            <strong>${escapeHtml(candidate.name)}</strong>
            <p>${escapeHtml(candidate.vision || "Candidate profile is ready for organization-specific manifesto content.")}</p>
        </article>
    `).join("");
    els.candidateSelect.innerHTML = state.candidates.map((candidate) => `
        <option value="${escapeHtml(candidate.id)}">${escapeHtml(candidate.name)}</option>
    `).join("");
}

function renderVoters() {
    if (!state.voters.length) {
        els.voterTable.innerHTML = `<tr><td colspan="5">No voters have been registered.</td></tr>`;
        return;
    }
    els.voterTable.innerHTML = state.voters.map((voter) => `
        <tr>
            <td>${escapeHtml(voter.name)}</td>
            <td>${escapeHtml(voter.email)}</td>
            <td>${escapeHtml(voter.role || "VOTER")}</td>
            <td><span class="badge ${voter.eligible ? "ok" : "danger"}">${voter.eligible ? "Eligible" : "Disabled"}</span></td>
            <td><button class="mini-button" type="button" data-toggle-voter="${escapeHtml(voter.id)}">${voter.eligible ? "Disable" : "Enable"}</button></td>
        </tr>
    `).join("");
}

function renderServices() {
    els.serviceList.innerHTML = state.services.map((service) => `
        <div class="service-item">
            <strong>${escapeHtml(service.service || service.name)}</strong>
            <span class="badge ok">${escapeHtml(service.status || "READY")}</span>
        </div>
    `).join("") || `<div class="empty-state">Service readiness is unavailable.</div>`;

    els.trustList.innerHTML = [
        ["Password policy", "Demo only"],
        ["Ballot writes", "One vote per election"],
        ["Result publication", state.results?.published ? "Published" : "Manual approval"],
        ["Audit events", "Required by PRD"]
    ].map(([label, value]) => `
        <li><strong>${escapeHtml(label)}</strong><span>${escapeHtml(value)}</span></li>
    `).join("");
}

function renderResults() {
    const result = state.results || { tally: [], published: false, electionId };
    const rows = result.tally || [];
    const total = rows.reduce((sum, row) => sum + Number(row.votes || 0), 0);
    els.resultElection.textContent = state.election?.name || result.electionId || "Election Results";
    els.resultPublication.textContent = result.published ? "Published result" : "Draft result snapshot";

    els.resultBars.innerHTML = rows.length ? rows.map((row) => {
        const votes = Number(row.votes || 0);
        const share = total === 0 ? 0 : Math.round((votes / total) * 100);
        const name = row.candidateName || row.candidate || row.candidateId;
        return `
            <div class="result-row">
                <header><span>${escapeHtml(name)}</span><span>${votes} votes · ${share}%</span></header>
                <div class="result-track"><span style="width: ${share}%"></span></div>
            </div>
        `;
    }).join("") : `<div class="empty-state">No tally rows are available yet.</div>`;

    els.resultSummary.innerHTML = [
        metric("Total votes", total),
        metric("Published", result.published ? "Yes" : "No"),
        metric("Election ID", result.electionId || electionId),
        metric("Turnout", `${percent(state.overview.turnoutPercent)}%`)
    ].join("");
}

async function loadBrand() {
    state.brand = await request("/api/config");
    renderBrand();
}

async function loadOverview() {
    state.overview = await request("/api/overview");
    renderOverview();
}

async function loadElection() {
    const elections = await request("/api/elections");
    state.election = elections[0] || null;
    state.candidates = state.election?.candidates || [];
    renderElection();
}

async function loadVoters() {
    state.voters = await request("/api/admin/voters");
    renderVoters();
}

async function loadServices() {
    state.services = await request("/api/services");
    renderServices();
}

async function loadResults() {
    state.results = await request(`/api/results/${electionId}`);
    renderResults();
    renderServices();
}

async function refreshAll() {
    await Promise.all([
        loadBrand(),
        loadOverview(),
        loadElection(),
        loadVoters(),
        loadServices(),
        loadResults()
    ]);
}

async function signIn(event) {
    event.preventDefault();
    setMessage(els.loginMessage, "Signing in...");
    try {
        const user = await request("/api/auth/login", {
            method: "POST",
            body: JSON.stringify({
                email: els.loginEmail.value,
                password: els.loginPassword.value
            })
        });
        writeSession(user);
        setMessage(els.loginMessage, `Welcome, ${user.name}.`, "success");
        location.hash = user.role === "ORG_ADMIN" ? "admin" : "vote";
    } catch (error) {
        setMessage(els.loginMessage, error.message, "error");
    }
}

async function castVote(event) {
    event.preventDefault();
    setMessage(els.voteMessage, "Submitting ballot...");
    try {
        const receipt = await request("/api/votes", {
            method: "POST",
            body: JSON.stringify({
                voterEmail: els.voteEmail.value,
                candidateId: els.candidateSelect.value
            })
        });
        setMessage(els.voteMessage, `Vote accepted. Receipt ${receipt.receiptId} for ${receipt.candidateId}.`, "success");
        await Promise.all([loadOverview(), loadResults()]);
    } catch (error) {
        setMessage(els.voteMessage, error.message, "error");
    }
}

async function addVoter(event) {
    event.preventDefault();
    try {
        await request("/api/admin/voters", {
            method: "POST",
            body: JSON.stringify({
                name: els.voterName.value,
                email: els.voterEmail.value,
                eligible: els.voterEligible.checked
            })
        });
        els.voterForm.reset();
        els.voterEligible.checked = true;
        await Promise.all([loadVoters(), loadOverview()]);
    } catch (error) {
        alert(error.message);
    }
}

async function toggleVoter(voterId) {
    const voter = state.voters.find((item) => item.id === voterId);
    if (!voter) return;
    await request(`/api/admin/voters/${voterId}`, {
        method: "PATCH",
        body: JSON.stringify({ eligible: !voter.eligible })
    });
    await Promise.all([loadVoters(), loadOverview()]);
}

async function publishResults() {
    els.publishResults.textContent = "Publishing...";
    try {
        await request(`/api/results/${electionId}/publish`, { method: "POST" });
        await loadResults();
        location.hash = "results";
    } catch (error) {
        alert(error.message);
    } finally {
        els.publishResults.textContent = "Publish Results";
    }
}

document.querySelectorAll("[data-fill-email]").forEach((button) => {
    button.addEventListener("click", () => {
        els.loginEmail.value = button.dataset.fillEmail;
        els.loginPassword.value = button.dataset.fillPassword;
        els.loginEmail.focus();
    });
});

els.authAction.addEventListener("click", () => {
    if (state.user) {
        writeSession(null);
        location.hash = "landing";
    } else {
        location.hash = "login";
    }
});
els.loginForm.addEventListener("submit", signIn);
els.voteForm.addEventListener("submit", castVote);
els.voterForm.addEventListener("submit", addVoter);
els.publishResults.addEventListener("click", publishResults);
els.refreshResults.addEventListener("click", loadResults);
els.voterTable.addEventListener("click", (event) => {
    const target = event.target.closest("[data-toggle-voter]");
    if (target) {
        toggleVoter(target.dataset.toggleVoter).catch((error) => alert(error.message));
    }
});
window.addEventListener("hashchange", route);

renderBrand();
renderSession();
route();
refreshAll().catch((error) => {
    els.overviewMetrics.innerHTML = `<div class="empty-state">${escapeHtml(error.message)}</div>`;
});
