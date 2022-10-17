# aws-lambda-guice
guice dependency injection for the standard lambda runtime.

Add this repositoy to your pom.xml file, implement a RequestHandler as normal, 
create a guice Module class that creates your handler and any dependencies and then
build your jar and configure the aws lambda with the following
1. handler: com.coderberry.aws.guice.GuiceRequestHandler::handleRequest
2. Set the follwing environment variables
  1. CB_GUICE_MODULE = the classname of the guice module that creates your handler
  2. CB_HANDLER_CLASS = the classname of the handler to instantiate using the module above. Requests will be delegated to this handler.
