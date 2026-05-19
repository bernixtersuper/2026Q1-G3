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
    image_tag     = string
    desired_count = number
  })
}

variable "ml_training" {
  type = object({
    schedule_expression = string
    schedule_enabled    = bool
    sqs_batch_size      = number
  })
}

