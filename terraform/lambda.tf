provider "aws" {
  region = "us-east-1"
}

resource "aws_lambda_function" "validate_cpf_lambda" {
  function_name    = "validate-cpf-lambda"
  role            = "arn:aws:iam::528465521739:role/LabRole"
  handler         = "com.example.fast_food_lambda.src.lambda.CpfLambdaHandler::handleRequest"
  runtime         = "java21"
  memory_size     = 512
  timeout         = 15
  filename        = "lambda.zip"
  source_code_hash = filebase64sha256("lambda.zip")
}

resource "aws_api_gateway_rest_api" "validate_cpf_api" {
  name        = "validate-cpf-api"
  description = "API Gateway para a Lambda de validação de CPF"
}

resource "aws_api_gateway_resource" "validate_cpf" {
  rest_api_id = aws_api_gateway_rest_api.validate_cpf_api.id
  parent_id   = aws_api_gateway_rest_api.validate_cpf_api.root_resource_id
  path_part   = "validate-cpf"
}

resource "aws_api_gateway_method" "post_validate_cpf" {
  rest_api_id   = aws_api_gateway_rest_api.validate_cpf_api.id
  resource_id   = aws_api_gateway_resource.validate_cpf.id
  http_method   = "POST"
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito_auth.id
}

resource "aws_api_gateway_integration" "lambda_post_validate_cpf" {
  rest_api_id             = aws_api_gateway_rest_api.validate_cpf_api.id
  resource_id             = aws_api_gateway_resource.validate_cpf.id
  http_method             = aws_api_gateway_method.post_validate_cpf.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.validate_cpf_lambda.invoke_arn
}

resource "aws_api_gateway_deployment" "deployment" {
  depends_on  = [aws_api_gateway_integration.lambda_post_validate_cpf]
  rest_api_id = aws_api_gateway_rest_api.validate_cpf_api.id
  stage_name  = "prod"
}

resource "aws_cognito_user_pool" "validate_cpf_user_pool" {
  name = "validate-cpf-users"
}

resource "aws_cognito_user_pool_client" "validate_cpf_client" {
  name         = "validate-cpf-client"
  user_pool_id = aws_cognito_user_pool.validate_cpf_user_pool.id
}

resource "aws_api_gateway_authorizer" "cognito_auth" {
  name          = "validate-cpf-authorizer"
  rest_api_id   = aws_api_gateway_rest_api.validate_cpf_api.id
  type          = "COGNITO_USER_POOLS"
  provider_arns = [aws_cognito_user_pool.validate_cpf_user_pool.arn]
}

resource "aws_lambda_permission" "apigateway_invoke_lambda" {
  statement_id  = "AllowAPIGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.validate_cpf_lambda.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.validate_cpf_api.execution_arn}/*/*"
}