variable "function_name" {
  type = string
}

variable "handler" {
  type = string
}

variable "source_dir" {
  type = string
}

variable "iam_role_arn" {
  type = string
}

variable "environment_variables" {
  type    = map(string)
  default = {}
}

variable "python_version" {
  type    = string
  default = "python3.12"
}

variable "timeout" {
  type    = number
  default = 60
}

variable "vpc_subnet_ids" {
  type    = list(string)
  default = null
}

variable "vpc_security_group_ids" {
  type    = list(string)
  default = null
}
