resource "aws_wafv2_web_acl" "alb" {
  count = var.waf.enabled ? 1 : 0

  name  = "${local.name_prefix}-alb"
  scope = "REGIONAL"

  default_action {
    allow {}
  }

  # Techo por IP — anti-abuso sin castigar NAT compartido ni uso normal del admin/menú.
  rule {
    name     = "rate-limit-global"
    priority = 1

    action {
      block {}
    }

    statement {
      rate_based_statement {
        limit              = var.waf.global_rate_limit
        aggregate_key_type = "IP"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "${local.name_prefix}-waf-global-rate"
      sampled_requests_enabled   = true
    }
  }

  # Payloads conocidos maliciosos (Log4j, etc.) — sin inspeccionar JSON de la API.
  rule {
    name     = "aws-managed-known-bad-inputs"
    priority = 2

    override_action {
      none {}
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesKnownBadInputsRuleSet"
        vendor_name = "AWS"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "${local.name_prefix}-waf-known-bad-inputs"
      sampled_requests_enabled   = true
    }
  }

  visibility_config {
    cloudwatch_metrics_enabled = true
    metric_name                = "${local.name_prefix}-waf"
    sampled_requests_enabled   = true
  }

  tags = {
    Name = "${local.name_prefix}-alb-waf"
  }
}

resource "aws_wafv2_web_acl_association" "alb" {
  count = var.waf.enabled ? 1 : 0

  resource_arn = aws_lb.backend.arn
  web_acl_arn  = aws_wafv2_web_acl.alb[0].arn
}
