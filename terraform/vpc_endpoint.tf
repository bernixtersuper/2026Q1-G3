resource "aws_vpc_endpoint" "gateway_endpoints" {
  for_each = toset(local.gateway_endpoints)

  vpc_id            = module.vpc.vpc_id
  service_name      = each.value
  vpc_endpoint_type = "Gateway"
  route_table_ids   = local.route_table_ids
}

resource "aws_security_group" "vpc_interface_endpoints" {
  name_prefix = "${local.name_prefix}-vpce-"
  description = "Interface VPC endpoints (Secrets Manager, SQS, ECR)"
  vpc_id      = module.vpc.vpc_id

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

resource "aws_vpc_security_group_ingress_rule" "vpce_from_db_client" {
  security_group_id            = aws_security_group.vpc_interface_endpoints.id
  from_port                    = 443
  to_port                      = 443
  ip_protocol                  = "tcp"
  referenced_security_group_id = aws_security_group.db_client.id
}

resource "aws_vpc_security_group_ingress_rule" "vpce_from_fargate" {
  security_group_id            = aws_security_group.vpc_interface_endpoints.id
  from_port                    = 443
  to_port                      = 443
  ip_protocol                  = "tcp"
  referenced_security_group_id = aws_security_group.fargate.id
}

resource "aws_vpc_security_group_ingress_rule" "vpce_from_rds_proxy" {
  security_group_id            = aws_security_group.vpc_interface_endpoints.id
  from_port                    = 443
  to_port                      = 443
  ip_protocol                  = "tcp"
  referenced_security_group_id = aws_security_group.proxy.id
}

resource "aws_vpc_endpoint" "secretsmanager" {
  vpc_id              = module.vpc.vpc_id
  service_name        = local.secretsmanager_endpoint_service
  vpc_endpoint_type   = "Interface"
  subnet_ids          = module.vpc.private_subnets
  security_group_ids  = [aws_security_group.vpc_interface_endpoints.id]
  private_dns_enabled = true

  tags = {
    Name = "${local.name_prefix}-secretsmanager"
  }
}

resource "aws_vpc_endpoint" "sqs" {
  vpc_id              = module.vpc.vpc_id
  service_name        = local.sqs_endpoint_service
  vpc_endpoint_type   = "Interface"
  subnet_ids          = module.vpc.private_subnets
  security_group_ids  = [aws_security_group.vpc_interface_endpoints.id]
  private_dns_enabled = true

  tags = {
    Name = "${local.name_prefix}-sqs"
  }
}


resource "aws_vpc_endpoint" "ecr_api" {
  vpc_id              = module.vpc.vpc_id
  service_name        = local.ecr_api_endpoint_service
  vpc_endpoint_type   = "Interface"
  subnet_ids          = module.vpc.private_subnets
  security_group_ids  = [aws_security_group.vpc_interface_endpoints.id]
  private_dns_enabled = true

  tags = {
    Name = "${local.name_prefix}-ecr-api"
  }
}

resource "aws_vpc_endpoint" "ecr_dkr" {
  vpc_id              = module.vpc.vpc_id
  service_name        = local.ecr_dkr_endpoint_service
  vpc_endpoint_type   = "Interface"
  subnet_ids          = module.vpc.private_subnets
  security_group_ids  = [aws_security_group.vpc_interface_endpoints.id]
  private_dns_enabled = true

  tags = {
    Name = "${local.name_prefix}-ecr-dkr"
  }
}
