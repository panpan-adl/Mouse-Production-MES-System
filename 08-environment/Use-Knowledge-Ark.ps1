param([switch]$NoPrompt)
$ErrorActionPreference = 'Stop'

# Run in the SAME PowerShell session that starts the backend. No key is written to disk.
$env:MES_KNOWLEDGE_ENABLED = 'true'
$env:MES_KNOWLEDGE_MODEL_BASE_URL = 'https://ark.cn-beijing.volces.com/api/v3'
$env:MES_KNOWLEDGE_MODEL = 'doubao-seed-2-1-lite-260915'

if (-not $env:MES_KNOWLEDGE_MODEL_API_KEY -and $env:ARK_API_KEY) {
    $env:MES_KNOWLEDGE_MODEL_API_KEY = $env:ARK_API_KEY
}
if (-not $env:MES_KNOWLEDGE_MODEL_API_KEY) {
    if ($NoPrompt) { throw 'Set ARK_API_KEY or MES_KNOWLEDGE_MODEL_API_KEY before running with -NoPrompt.' }
    $arkSecureKey = Read-Host 'Enter your Volcano Ark API key (input is hidden)' -AsSecureString
    $arkKeyPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($arkSecureKey)
    try { $env:MES_KNOWLEDGE_MODEL_API_KEY = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($arkKeyPointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($arkKeyPointer); $arkSecureKey.Dispose() }
}
if ([string]::IsNullOrWhiteSpace($env:MES_KNOWLEDGE_MODEL_API_KEY)) { throw 'API key cannot be empty.' }
if (-not $env:MES_EMBED_API_KEY) { $env:MES_EMBED_API_KEY = $env:MES_KNOWLEDGE_MODEL_API_KEY }
if (-not $env:MES_EMBED_BASE_URL) { $env:MES_EMBED_BASE_URL = $env:MES_KNOWLEDGE_MODEL_BASE_URL }
if (-not $env:MES_EMBED_MODEL) { $env:MES_EMBED_MODEL = 'doubao-embedding-vision-251215' }
if (-not $env:MES_RAG_STORAGE) { $env:MES_RAG_STORAGE = Join-Path (Split-Path $PSScriptRoot -Parent) '.runtime\rag-files' }
Write-Host 'Knowledge model: Volcano Ark / doubao-seed-2-1-lite-260915'
Write-Host 'Embedding model: doubao-embedding-vision-251215 (requires model access on this API key).'
Write-Host 'Model settings are ready in this PowerShell session. API key is not displayed or saved.'
Write-Host 'Next configure Neo4j if needed, then restart the backend from this SAME session.'
