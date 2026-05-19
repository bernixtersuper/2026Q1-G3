resource "aws_security_group" "fargate" {
  name_prefix = "${local.name_prefix}-fargate-"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description     = "Quarkus desde ALB"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_ecs_cluster" "backend" {
  name = "${local.name_prefix}-cluster"
}

resource "aws_ecs_task_definition" "backend" {
  family                   = "${local.name_prefix}-backend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = local.lab_role_arn
  task_role_arn            = local.lab_role_arn

  container_definitions = jsonencode([{
    name      = "backend"
    image     = local.backend_image
    essential = true

    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]

    environment = [
      { name = "DB_URL", value = local.db_jdbc_url },
      { name = "DB_SECRET_ARN", value = aws_db_instance.db.master_user_secret[0].secret_arn },
      { name = "DB_SECRET_CACHE_SECONDS", value = "300" },
      { name = "AWS_REGION", value = data.aws_region.current.region },
      { name = "S3_BUCKET", value = local.images_bucket_name },
      { name = "DYNAMO_TABLE", value = aws_dynamodb_table.menuqr_events.name },
      { name = "QUARKUS_PROFILE", value = "prod" },
      { name = "RECOMMENDATIONS_MODEL_S3_BUCKET", value = local.ml_bucket_name },
      { name = "COGNITO_ISSUER_URL", value = "https://cognito-idp.${data.aws_region.current.region}.amazonaws.com/${aws_cognito_user_pool.main.id}" },
      { name = "COGNITO_CLIENT_ID", value = aws_cognito_user_pool_client.admin_spa.id },
    ]
  }])
}

resource "aws_ecs_service" "backend" {
  name            = "${local.name_prefix}-backend"
  cluster         = aws_ecs_cluster.backend.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = var.backend.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = module.vpc.private_subnets
    security_groups  = [aws_security_group.fargate.id, aws_security_group.db_client.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.backend.arn
    container_name   = "backend"
    container_port   = 8080
  }

  depends_on = [aws_lb_listener.backend_http]

  lifecycle {
    ignore_changes = [desired_count]
  }
}
