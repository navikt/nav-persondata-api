#!/bin/bash
set -euo pipefail

echo "🚀 Setting up environment for nav-persondata-api…"

# 🔧 Ensure kubectl is configured for the correct cluster and namespace
echo "🔧 Configuring kubectl..."

if ! command -v gke-gcloud-auth-plugin &> /dev/null; then
    echo "⚠️ gke-gcloud-auth-plugin is not installed. Installing it..."
    gcloud components install gke-gcloud-auth-plugin
fi

kubectl config use-context dev-gcp
if ! kubectl config view &>/dev/null; then
    echo "❌ Failed to configure kubectl. Ensure you are logged in to Google Cloud."
    exit 1
fi

kubectl config set-context --current --namespace=holmes

# ===== Konfig =====
SECRETS_FILE="src/main/resources/.env.local.properties"
NAMESPACE="holmes"

AZURE_SECRET_PREFIX="azure-nav-persondata-api"
VALKEY_SECRET_PREFIX="aiven-valkey-nav-persondata-api-nav-persondat"

AZURE_VARS=(
  AZURE_APP_CLIENT_ID
  AZURE_APP_CLIENT_SECRET
)

VALKEY_VARS=(
  VALKEY_PORT_NAV_PERSONDATA_API
  VALKEY_HOST_NAV_PERSONDATA_API
  VALKEY_USERNAME_NAV_PERSONDATA_API
  VALKEY_PASSWORD_NAV_PERSONDATA_API
)

# ===== Overwrite-sjekk =====
if [ -f "$SECRETS_FILE" ]; then
    read -p "⚠️ $SECRETS_FILE already exists. Do you want to overwrite it? (y/n): " choice
    case "$choice" in
      y|Y ) echo "🔄 Overwriting $SECRETS_FILE...";;
      n|N ) echo "✅ Keeping the existing $SECRETS_FILE."; exit 0;;
      * ) echo "❌ Invalid input. Exiting..."; exit 1;;
    esac
fi

# ===== Finn secrets dynamisk =====
echo "🔍 Locating Kubernetes Secrets..."

AZURE_SECRET_NAME=$(kubectl get secrets -n "$NAMESPACE" \
  | awk "/^$AZURE_SECRET_PREFIX/ { print \$1; exit }")

VALKEY_SECRET_NAME=$(kubectl get secrets -n "$NAMESPACE" \
  | awk "/^$VALKEY_SECRET_PREFIX/ { print \$1; exit }")

if [ -z "$AZURE_SECRET_NAME" ]; then
  echo "❌ Fant ingen Azure secret som starter med $AZURE_SECRET_PREFIX"
  exit 1
fi

if [ -z "$VALKEY_SECRET_NAME" ]; then
  echo "❌ Fant ingen Valkey secret som starter med $VALKEY_SECRET_PREFIX"
  exit 1
fi

echo "✅ Azure secret:  $AZURE_SECRET_NAME"
echo "✅ Valkey secret: $VALKEY_SECRET_NAME"

# ===== Hent secrets =====
echo "🔑 Fetching secrets..."
echo "# Autogenerert fra Kubernetes Secrets" > "$SECRETS_FILE"

fetch_vars () {
  local SECRET_NAME=$1
  shift
  local VARS=("$@")

  for VAR in "${VARS[@]}"; do
    VALUE=$(kubectl get secret "$SECRET_NAME" -n "$NAMESPACE" \
      -o "jsonpath={.data.$VAR}" 2>/dev/null || true)

    if [ -z "$VALUE" ]; then
      echo "⚠️ Mangler $VAR i secret $SECRET_NAME" >&2
      continue
    fi

    DECODED=$(printf '%s' "$VALUE" | base64 --decode)
    echo "$VAR=$DECODED" >> "$SECRETS_FILE"
  done
}

fetch_vars "$AZURE_SECRET_NAME" "${AZURE_VARS[@]}"
fetch_vars "$VALKEY_SECRET_NAME" "${VALKEY_VARS[@]}"

echo "✅ Secrets saved to $SECRETS_FILE"

# ===== Git ignore =====
if ! grep -q "^$SECRETS_FILE$" .gitignore 2>/dev/null; then
    echo "🚨 Adding $SECRETS_FILE to .gitignore..."
    echo "$SECRETS_FILE" >> .gitignore
fi

echo "🎉 Done!"

