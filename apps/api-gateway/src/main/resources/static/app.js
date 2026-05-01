const view = {
    brandName: document.getElementById("brand-name"),
    orgName: document.getElementById("org-name"),
    headline: document.getElementById("headline"),
    overview: document.getElementById("overview"),
    loginForm: document.getElementById("login-form"),
    loginOutput: document.getElementById("login-output"),
    voteForm: document.getElementById("vote-form"),
    voteOutput: document.getElementById("vote-output"),
    candidateBox: document.getElementById("candidates"),
    candidateSelect: document.getElementById("candidate-id"),
    voterForm: document.getElementById("voter-form"),
    voterList: document.getElementById("voter-list"),
    publishBtn: document.getElementById("publish-btn")
};

const electionId = "ovs-board-2026";

function print(target, data) {
    target.textContent = JSON.stringify(data, null, 2);
}

async function request(url, options = {}) {
    const response = await fetch(url, {
        headers: { "Content-Type": "application/json" },
        ...options
    });
    const payload = await response.json();
    if (!response.ok) {
        throw new Error(payload.message || "Request failed.");
    }
    return payload;
}

function asPill(label, value) {
    const node = document.createElement("span");
    node.className = "pill";
    node.textContent = `${label}: ${value}`;
    return node;
}

async function loadBrand() {
    const payload = await request("/api/config");
    const brand = payload.data;
    document.title = `${brand.appName} - ${brand.organizationName}`;
    document.documentElement.style.setProperty("--accent", brand.accentColor);
    view.brandName.textContent = brand.appName;
    view.orgName.textContent = brand.organizationName;
    view.headline.textContent = "Organization Voting System";
}

async function loadOverview() {
    const payload = await request("/api/overview");
    const data = payload.data;
    view.overview.innerHTML = "";
    view.overview.appendChild(asPill("Election", data.activeElection));
    view.overview.appendChild(asPill("Status", data.status));
    view.overview.appendChild(asPill("Eligible Voters", data.eligibleVoters));
    view.overview.appendChild(asPill("Votes Cast", data.votesCast));
    view.overview.appendChild(asPill("Turnout", `${data.turnoutPercent}%`));
}

async function loadCandidates() {
    const payload = await request("/api/elections");
    const election = payload.data[0];
    const candidates = election.candidates || [];
    view.candidateBox.innerHTML = "";
    view.candidateSelect.innerHTML = "";
    candidates.forEach((candidate) => {
        const item = document.createElement("div");
        item.className = "candidate-item";
        item.textContent = `${candidate.name} (${candidate.id})`;
        view.candidateBox.appendChild(item);

        const option = document.createElement("option");
        option.value = candidate.id;
        option.textContent = candidate.name;
        view.candidateSelect.appendChild(option);
    });
}

async function loadVoters() {
    const payload = await request("/api/admin/voters");
    view.voterList.innerHTML = "";
    payload.data.forEach((voter) => {
        const item = document.createElement("div");
        item.className = "voter-item";
        item.innerHTML = `${voter.name} (${voter.email}) - eligible: <strong>${voter.eligible}</strong>`;

        const toggle = document.createElement("button");
        toggle.type = "button";
        toggle.className = "outline";
        toggle.textContent = voter.eligible ? "Disable" : "Enable";
        toggle.onclick = async () => {
            await request(`/api/admin/voters/${voter.id}`, {
                method: "PATCH",
                body: JSON.stringify({ eligible: !voter.eligible })
            });
            await Promise.all([loadVoters(), loadOverview()]);
        };
        item.appendChild(toggle);
        view.voterList.appendChild(item);
    });
}

view.loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
        const payload = await request("/api/auth/login", {
            method: "POST",
            body: JSON.stringify({
                email: document.getElementById("email").value,
                password: document.getElementById("password").value
            })
        });
        print(view.loginOutput, payload);
    } catch (error) {
        print(view.loginOutput, { success: false, message: error.message });
    }
});

view.voteForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
        const payload = await request("/api/votes", {
            method: "POST",
            body: JSON.stringify({
                voterEmail: document.getElementById("voter-email").value,
                candidateId: view.candidateSelect.value
            })
        });
        print(view.voteOutput, payload);
        await loadOverview();
    } catch (error) {
        print(view.voteOutput, { success: false, message: error.message });
    }
});

view.voterForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    await request("/api/admin/voters", {
        method: "POST",
        body: JSON.stringify({
            name: document.getElementById("voter-name").value,
            email: document.getElementById("voter-email-new").value,
            eligible: document.getElementById("voter-eligible").checked
        })
    });
    view.voterForm.reset();
    document.getElementById("voter-eligible").checked = true;
    await Promise.all([loadVoters(), loadOverview()]);
});

view.publishBtn.addEventListener("click", async () => {
    const payload = await request(`/api/results/${electionId}/publish`, { method: "POST" });
    print(view.voteOutput, payload);
});

Promise.all([loadBrand(), loadOverview(), loadCandidates(), loadVoters()]).catch((error) => {
    view.voteOutput.textContent = error.message;
});
