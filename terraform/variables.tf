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

variable "glue_analytics" {
  type = object({
    enrich_schedule_expression = string
    enrich_schedule_enabled    = bool
    crawler_schedule           = string
  })
  default = {
    enrich_schedule_expression = "cron(0 3 * * ? *)"
    enrich_schedule_enabled    = true
    crawler_schedule           = "cron(0 4 * * ? *)"
  }
  description = <<-EOT
    Glue analytics: job enrich (S3 Parquet → Dynamo DAY#) y crawler del catálogo.
    Para demo manual preferí RUN_GLUE=1 bash terraform/scripts/run-glue-analytics-enrich.sh.
    Ejemplo schedule más frecuente: enrich_schedule_expression = "cron(0/30 * * * ? *)" (cada 30 min).
  EOT
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


variable "demo_seed_enabled" {
  type        = bool
  default     = false
  description = "Enables the hidden POST /api/admin/demo/seed endpoint that populates the caller's tenant with demo data. Keep false in real environments; set true (via tfvars) for a presentation."
}
