param(
    [switch]$NoPrompt,
    [string]$ConfigPath = (Join-Path $PSScriptRoot 'knowledge.local.json')
)
$ErrorActionPreference = 'Stop'

# Explicit environment settings take precedence over the optional, Git-ignored local file.
$arkLocalConfig = $null
if (Test-Path -LiteralPath $ConfigPath) {
    try { $arkLocalConfig = Get-Content -LiteralPath $ConfigPath -Raw -Encoding UTF8 | ConvertFrom-Json }
    catch { throw 'Cannot read knowledge.local.json. Check JSON syntax (double quotes and commas); file contents are not displayed.' }
    if ($null -eq $arkLocalConfig -or $arkLocalConfig -is [array] -or $arkLocalConfig -isnot [pscustomobject]) {
        throw 'knowledge.local.json must contain one JSON configuration object.'
    }
    foreach ($arkMapping in @(
        @('baseUrl','MES_KNOWLEDGE_MODEL_BASE_URL'),
        @('model','MES_KNOWLEDGE_MODEL'),
        @('apiKey','MES_KNOWLEDGE_MODEL_API_KEY'),
        @('embeddingBaseUrl','MES_EMBED_BASE_URL'),
        @('embeddingModel','MES_EMBED_MODEL'),
        @('embeddingApiKey','MES_EMBED_API_KEY')
    )) {
        $arkProperty = $arkLocalConfig.PSObject.Properties[$arkMapping[0]]
        if ($arkProperty -and $null -ne $arkProperty.Value -and $arkProperty.Value -isnot [string]) {
            throw "Configuration field $($arkMapping[0]) must be a string."
        }
        $arkValue = if ($arkProperty) { [string]$arkProperty.Value } else { '' }
        if (-not [Environment]::GetEnvironmentVariable($arkMapping[1], 'Process') -and -not [string]::IsNullOrWhiteSpace($arkValue)) {
            [Environment]::SetEnvironmentVariable($arkMapping[1], $arkValue.Trim(), 'Process')
        }
    }
}
$env:MES_KNOWLEDGE_ENABLED = 'true'
if (-not $env:MES_KNOWLEDGE_MODEL_BASE_URL) { $env:MES_KNOWLEDGE_MODEL_BASE_URL = 'https://ark.cn-beijing.volces.com/api/v3' }
if (-not $env:MES_KNOWLEDGE_MODEL) { $env:MES_KNOWLEDGE_MODEL = 'doubao-seed-2-1-lite-260915' }

if (-not $env:MES_KNOWLEDGE_MODEL_API_KEY -and $env:ARK_API_KEY) {
    $env:MES_KNOWLEDGE_MODEL_API_KEY = $env:ARK_API_KEY
}
if (-not $env:MES_KNOWLEDGE_MODEL_API_KEY) {
    if ($NoPrompt) { throw 'Fill apiKey in 08-environment/knowledge.local.json, or set ARK_API_KEY / MES_KNOWLEDGE_MODEL_API_KEY.' }
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
Write-Host "Knowledge model: Volcano Ark / $env:MES_KNOWLEDGE_MODEL"
Write-Host "Embedding model: $env:MES_EMBED_MODEL (requires model access on this API key)."
Write-Host 'Model settings are ready. API key is not displayed; local configuration is excluded from Git.'
Write-Host 'Next configure Neo4j if needed, then restart the backend from this SAME session.'
# Avoid retaining an extra copy of local secrets in the caller's script variables.
Remove-Variable arkLocalConfig,arkValue,arkProperty -ErrorAction SilentlyContinue
