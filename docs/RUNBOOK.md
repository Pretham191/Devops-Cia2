# BeforeYouGo — CI/CD Runbook

Every command to take the app from this folder to a live AKS deployment, in order.
Run commands in **WSL Ubuntu** unless marked **PowerShell**.
📸 marks a screenshot needed for the report.

| Name | Value |
|---|---|
| Region | `koreacentral` |
| Resource group | `rg-beforeyougo` |
| Container registry | `beforeyougoacr` (must be globally unique — see step 1) |
| AKS cluster | `aks-beforeyougo` (1 × `Standard_B2as_v2`, free tier) |
| Namespace | `beforeyougo` |

---

## 1. Create the Azure resources (≈10–15 min)

```bash
az login --use-device-code          # if not already logged in
RG=rg-beforeyougo
LOC=koreacentral
ACR=beforeyougoacr
AKS=aks-beforeyougo

az acr check-name -n $ACR -o table  # NameAvailable must be True; otherwise pick another name

az group create -n $RG -l $LOC
az acr create -g $RG -n $ACR --sku Basic -l $LOC
az aks create -g $RG -n $AKS -l $LOC \
  --tier free --node-count 1 --node-vm-size Standard_B2as_v2 \
  --attach-acr $ACR --generate-ssh-keys
```

If you had to change the registry name, also change `ACR_NAME` at the top of `Jenkinsfile`.

Check the cluster works:
```bash
az aks get-credentials -g $RG -n $AKS --overwrite-existing
kubectl get nodes -o wide
```
📸 Azure portal → resource group `rg-beforeyougo` (shows ACR + AKS), and the AKS cluster overview page.

## 2. Let the `jenkins` user reach Azure

Jenkins runs as its own Linux user, so it needs its own Azure login.

**Preferred: a service principal scoped to the resource group only.**
```bash
SUB=$(az account show --query id -o tsv)
az ad sp create-for-rbac --name sp-beforeyougo-jenkins \
  --role Contributor --scopes /subscriptions/$SUB/resourceGroups/rg-beforeyougo
# note appId, password, tenant from the output (never commit them)

sudo -u jenkins -H az login --service-principal \
  -u <appId> -p <password> --tenant <tenant>
```

**If `create-for-rbac` fails with "Insufficient privileges"** (university tenants often block app registrations), log the jenkins user in as yourself instead:
```bash
sudo -u jenkins -H az login --use-device-code
```

Verify either way:
```bash
sudo -u jenkins -H az account show -o table
sudo -u jenkins -H az acr login -n beforeyougoacr
```

## 3. Push the code to GitHub

Create an empty **public** repo on GitHub named `beforeyougo` (no README). Then, in the project folder (PowerShell or WSL):
```bash
git init
git add .
git commit -m "BeforeYouGo app with CI/CD pipeline"
git branch -M main
git remote add origin https://github.com/<your-username>/beforeyougo.git
git push -u origin main
```

## 4. Configure Jenkins (http://localhost:8080)

**Plugins** (Manage Jenkins → Plugins → Available): make sure these are installed —
GitHub, Pipeline, Git, Credentials Binding, JUnit, Coverage, Timestamper, **Pipeline Graph View** (nice stage view for screenshots).

**Credentials** (Manage Jenkins → Credentials → System → Global → Add):
| ID | Kind | Value |
|---|---|---|
| `byg-db-password` | Secret text | a strong password for Postgres — **never change it after the first deploy** |
| `byg-admin` | Username with password | the app's admin login (e.g. `admin` / a strong password) |

**Job**: New Item → name `beforeyougo` → **Pipeline** →
- General: tick **GitHub project**, URL `https://github.com/<your-username>/beforeyougo/`
- Triggers: tick **GitHub hook trigger for GITScm polling**
- Pipeline: **Pipeline script from SCM** → SCM **Git** → repo URL `https://github.com/<your-username>/beforeyougo.git` → branch `*/main` → Script Path `Jenkinsfile`
- Save.

## 5. Webhook through ngrok

**PowerShell** (keep this window open during the demo):
```powershell
ngrok http --url=<your-ngrok-domain> 8080
```
GitHub repo → Settings → Webhooks → Add webhook:
- Payload URL: `https://<your-ngrok-domain>/github-webhook/` (trailing slash matters)
- Content type: `application/json`
- Events: **Just the push event** → Add.

GitHub sends a ping; it should show a green tick under **Recent Deliveries**.

## 6. First pipeline run

Click **Build Now** once (this registers the push trigger). The first run takes longer: Maven downloads dependencies and Azure creates the Load Balancer.

When it's green, get the URL:
```bash
kubectl -n beforeyougo get svc beforeyougo     # EXTERNAL-IP (app is also at http://beforeyougo-cia2-pretham.koreacentral.cloudapp.azure.com/)
kubectl -n beforeyougo get deploy,pods -o wide
```
📸 Jenkins stage view (all green) · Test Result page · Coverage page
📸 ACR → Repositories → `beforeyougo` (tags like `1-abc1234`, `latest`)
📸 `kubectl get deploy,pods,svc` showing pods **Running** and deployment **2/2 READY**
📸 Browser at `http://<EXTERNAL-IP>/`, footer showing `build <tag>`

## 7. Change-and-redeploy demo

1. Make a visible change, e.g. edit the tagline in `src/main/resources/templates/index.html`.
2. Commit and push:
   ```bash
   git commit -am "Update home page tagline"
   git push
   ```
3. Jenkins starts **by itself** (build cause: "Started by GitHub push").
4. Watch the rollout: `kubectl -n beforeyougo get pods -w` — new pods start, old ones terminate, no downtime.
5. Refresh the browser: new tagline + new build number in the footer.

📸 GitHub commit · Jenkins build "Started by GitHub push" · new tag in ACR · new pods · updated page

## 8. Useful checks

```bash
kubectl -n beforeyougo logs deploy/beforeyougo --tail=50
kubectl -n beforeyougo describe pod -l app=beforeyougo
kubectl -n beforeyougo rollout history deployment/beforeyougo
kubectl -n beforeyougo rollout undo deployment/beforeyougo   # roll back one version
```

## 9. Clean up after the viva (stops credit usage)

```bash
az group delete -n rg-beforeyougo --yes --no-wait
```
