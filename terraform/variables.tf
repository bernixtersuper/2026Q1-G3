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

variable "ml_training" {
  type = object({
    schedule_expression = string
    schedule_enabled    = bool
    sqs_batch_size      = number
  })
}

