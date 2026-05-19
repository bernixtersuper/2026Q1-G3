data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_iam_role" "lab_role" {
  name = "LabRole"
}

data "aws_region" "current" {}

data "aws_caller_identity" "current" {}