variable "aws_region" {
  type = string
}

variable "project_name" {
  type = string
}

variable "vpc_cidr" {
  type = string
}

variable "images_bucket_name" { type = string }
variable "ml_bucket_name" { type = string }
variable "user_website_name" { type = string }
variable "admin_website_name" { type = string }

variable "db" {
  type = object({
    name     = string
    username = string
  })
}

variable "backend" {
  type = object({
    image_tag              = string
    desired_count          = number
    autoscaling_min        = optional(number, 2)
    autoscaling_max        = optional(number, 4)
    autoscaling_target_cpu = optional(number, 70)
  })

  validation {
    condition     = var.backend.autoscaling_min <= var.backend.autoscaling_max
    error_message = "backend.autoscaling_min must be less than or equal to backend.autoscaling_max."
  }

  validation {
    condition     = var.backend.desired_count >= var.backend.autoscaling_min && var.backend.desired_count <= var.backend.autoscaling_max
    error_message = "backend.desired_count must be between autoscaling_min and autoscaling_max."
  }
}

variable "alert_email" {
  type        = string
  default     = null
  nullable    = true
  description = "Email para alertas operativas (DLQ, etc.). Requiere confirmar la suscripción SNS desde el correo."
}

variable "log_retention_in_days" {
  type        = number
  default     = 14
  description = "Retención de log groups CloudWatch (ECS y Lambdas)."
}

variable "ml_training" {
  type = object({
    schedule_expression   = string
    schedule_enabled      = bool
    sqs_batch_size        = number
    sqs_max_receive_count = optional(number, 3)
  })
}

variable "waf" {
  type = object({
    enabled           = optional(bool, true)
    global_rate_limit = optional(number, 5000)
  })
  default = {
    enabled           = true
    global_rate_limit = 5000
  }

  description = "WAF regional en el ALB. global_rate_limit = requests por IP en ventana de 5 minutos."
}

variable "cognito_domain_prefix" {
  type        = string
  description = "Prefijo del dominio Hosted UI de Cognito (https://<prefix>.auth.<region>.amazoncognito.com). Debe ser único por región."
  default     = "menuqr-g3-auth"
}

variable "google_oauth" {
  type = object({
    client_id     = string
    client_secret = string
  })
  description = <<-EOT
    Credenciales OAuth 2.0 de Google Cloud Console para el login federado en Cognito.
    NO commitear el secret: proveer via `TF_VAR_google_oauth` o un archivo `*.auto.tfvars` ignorado por git
    (ver google.auto.tfvars.example). Con cadenas vacías no se crea el IdP de Google.
  EOT
  sensitive   = true
  default = {
    client_id     = ""
    client_secret = ""
  }
}

