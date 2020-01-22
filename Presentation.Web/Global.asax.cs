using System.Web.Http;
using System.Web.Mvc;
using System.Web.Optimization;
using System.Web.Routing;
using System.Web.Http.ExceptionHandling;
using System.Web.Http.Controllers;
using System.Net.Http.Headers;
using System.Diagnostics;
using ActionFilterAttribute = System.Web.Http.Filters.ActionFilterAttribute;
using System.Net;

namespace OS2Indberetning
{
    public class WebApiApplication : System.Web.HttpApplication
    {
        protected void Application_Start()
        {
            ServicePointManager.SecurityProtocol |= SecurityProtocolType.Tls12;

            AreaRegistration.RegisterAllAreas();
            GlobalConfiguration.Configure(WebApiConfig.Register);
            FilterConfig.RegisterGlobalFilters(GlobalFilters.Filters);
            RouteConfig.RegisterRoutes(RouteTable.Routes);
            BundleConfig.RegisterBundles(BundleTable.Bundles);

#if DEBUG
            GlobalConfiguration.Configuration.Services.Add(typeof(IExceptionLogger), new DebugExceptionLogger());
            GlobalConfiguration.Configuration.Filters.Add(new AddAcceptCharsetHeaderActionFilter());
#endif

            //// Turns off self reference looping when serializing models in API controlllers
            //GlobalConfiguration.Configuration.Formatters.JsonFormatter.SerializerSettings.ReferenceLoopHandling = ReferenceLoopHandling.Ignore;

            //// Set JSON serialization in WEB API to use camelCase (javascript) instead of PascalCase (C#)
            //GlobalConfiguration.Configuration.Formatters.JsonFormatter.SerializerSettings.ContractResolver = new CamelCasePropertyNamesContractResolver();
        }
    }

#if DEBUG
    public class AddAcceptCharsetHeaderActionFilter : ActionFilterAttribute
    {
        public override void OnActionExecuting(HttpActionContext actionContext)
        {
            // Inject "Accept-Charset" header into the client request,
            // since apparently this is required when running on Mono
            // See: https://github.com/OData/odata.net/issues/165
            actionContext.Request.Headers.AcceptCharset.Add(new StringWithQualityHeaderValue("UTF-8"));
            base.OnActionExecuting(actionContext);
        }
    }

    public class DebugExceptionLogger : ExceptionLogger
    {
        public override void Log(ExceptionLoggerContext context)
        {
            Debug.WriteLine(context.Exception);
        }
    }
#endif
}
