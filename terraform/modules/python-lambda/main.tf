locals {
  filename = "${path.cwd}/lambda_${var.function_name}.zip"
}

data "archive_file" "lambda_zip" {
  type        = "zip"
  source_dir  = var.source_dir
  output_path = local.filename
}

resource "aws_lambda_function" "this" {
  function_name = var.function_name
  role          = var.iam_role_arn
  runtime       = var.python_version
  architectures = ["x86_64"]
  handler       = var.handler

  filename         = data.archive_file.lambda_zip.output_path
  source_code_hash = data.archive_file.lambda_zip.output_base64sha256

  timeout = var.timeout

  dynamic "vpc_config" {
    for_each = var.vpc_subnet_ids != null ? [1] : []
    content {
      subnet_ids         = var.vpc_subnet_ids
      security_group_ids = coalesce(var.vpc_security_group_ids, [])
    }
  }

  environment {
    variables = var.environment_variables
  }
}
