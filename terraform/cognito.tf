locals {
  # Google federation se habilita solo cuando se proveen ambas credenciales.
  google_oauth_enabled = var.google_oauth.client_id != "" && var.google_oauth.client_secret != ""

  admin_website_url = trimsuffix(module.s3-public-websites[var.admin_website_name].bucket_website_url, "/")

  # URLs exactas que el SPA usa como redirectSignIn / redirectSignOut.
  # Se incluye localhost para correr el SPA con `npm run dev` durante la verificación manual.
  cognito_callback_urls = [
    "${local.admin_website_url}/auth/callback",
    "http://localhost:5173/auth/callback",
  ]
  cognito_logout_urls = [
    "${local.admin_website_url}/login",
    "http://localhost:5173/login",
  ]

  cognito_identity_providers = concat(
    ["COGNITO"],
    local.google_oauth_enabled ? ["Google"] : [],
  )
}

resource "aws_cognito_user_pool" "main" {
  name = "${local.name_prefix}-users"

  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]

  email_configuration {
    email_sending_account = "COGNITO_DEFAULT"
  }

  admin_create_user_config {
    allow_admin_create_user_only = false
  }

  password_policy {
    minimum_length    = 12
    require_lowercase = true
    require_uppercase = true
    require_numbers   = true
    require_symbols   = true
  }

  mfa_configuration = "OPTIONAL"

  software_token_mfa_configuration {
    enabled = true
  }

  account_recovery_setting {
    recovery_mechanism {
      name     = "verified_email"
      priority = 1
    }
  }

  verification_message_template {
    default_email_option = "CONFIRM_WITH_CODE"
    email_subject        = "Verify your MenuQR account"
    email_message        = "Your MenuQR verification code is {####}. It expires in 24 hours."
  }
}

# Hosted UI domain: requerido para el flujo OAuth (Authorization Code) con
# proveedores federados como Google.
resource "aws_cognito_user_pool_domain" "main" {
  domain       = var.cognito_domain_prefix
  user_pool_id = aws_cognito_user_pool.main.id
}

# Identity Provider de Google. Solo se crea si se proveen las credenciales OAuth.
resource "aws_cognito_identity_provider" "google" {
  count = local.google_oauth_enabled ? 1 : 0

  user_pool_id  = aws_cognito_user_pool.main.id
  provider_name = "Google"
  provider_type = "Google"

  provider_details = {
    client_id        = var.google_oauth.client_id
    client_secret    = var.google_oauth.client_secret
    authorize_scopes = "openid email profile"
  }

  # Mapea los claims de Google a los atributos del user pool.
  # `email` es el username attribute, por lo que debe mapearse siempre.
  attribute_mapping = {
    email          = "email"
    email_verified = "email_verified"
    username       = "sub"
    name           = "name"
  }
}

resource "aws_cognito_user_pool_client" "admin_spa" {
  name         = "${local.name_prefix}-admin-spa"
  user_pool_id = aws_cognito_user_pool.main.id

  generate_secret              = false
  supported_identity_providers = local.cognito_identity_providers

  # OAuth (Authorization Code + PKCE desde el SPA público, sin secret).
  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows                  = ["code"]
  allowed_oauth_scopes                 = ["openid", "email", "profile"]
  callback_urls                        = local.cognito_callback_urls
  logout_urls                          = local.cognito_logout_urls

  explicit_auth_flows = [
    "ALLOW_USER_SRP_AUTH",
    "ALLOW_REFRESH_TOKEN_AUTH",
  ]

  access_token_validity  = 60
  id_token_validity      = 60
  refresh_token_validity = 30

  token_validity_units {
    access_token  = "minutes"
    id_token      = "minutes"
    refresh_token = "days"
  }

  enable_token_revocation       = true
  prevent_user_existence_errors = "ENABLED"

  # El client referencia "Google" en supported_identity_providers,
  # por lo que el IdP debe existir primero.
  depends_on = [aws_cognito_identity_provider.google]
}
