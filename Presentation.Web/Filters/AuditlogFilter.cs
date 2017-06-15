using Core.ApplicationServices.Logger;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using System.Web.Http.Filters;
using System.Web.Http.Controllers;

namespace OS2Indberetning.Filters
{
    public class AuditlogFilter : ActionFilterAttribute
    {
        private readonly ILogger _logger;

        public AuditlogFilter()
        {
            _logger = new Logger();
        }

        public override void OnActionExecuting(HttpActionContext actionContext)
        {
            System.Diagnostics.Trace.TraceInformation("Executing!!!");
            var user = HttpContext.Current.User.Identity.Name;
            var location = HttpContext.Current.Request.UserHostAddress;
            var controller = actionContext.RequestContext.RouteData.Values["controller"].ToString();
            var action = actionContext.RequestContext.RouteData.Values["action"].ToString();
            var parameters = JsonConvert.SerializeObject(actionContext.ActionArguments);

            _logger.AuditLog($"{DateTime.Now.Date} - {user} - {location} - {controller} - {action} - {parameters}");
            base.OnActionExecuting(actionContext);
        }

        
        //public override void OnActionExecuted(HttpActionExecutedContext actionExecutedContext)
        //{
        //    var user = HttpContext.Current.User.Identity.Name;
        //    var location = HttpContext.Current.Request.UserHostAddress;
        //    var controller = actionExecutedContext.ActionContext.ControllerContext.RouteData.Values["controller"].ToString();
        //    var action = actionExecutedContext.ActionContext.ControllerContext.RouteData.Values["action"].ToString();
        //    var parameters = JsonConvert.SerializeObject(actionExecutedContext.ActionContext);

        //    _logger.AuditLog($"{DateTime.Now.Date} - {user} - {location} - {controller} - {action} - {parameters}");
        //    base.OnActionExecuted(actionExecutedContext);
        //}

        //public void OnActionExecuting(ActionExecutingContext filterContext)
        //{
        //    var user = filterContext.HttpContext.User.Identity.Name;
        //    var location = filterContext.HttpContext.Request.UserHostAddress;
        //    var controller = filterContext.RouteData.Values["controller"].ToString();
        //    var action = filterContext.RouteData.Values["action"].ToString();
        //    var parameters = JsonConvert.SerializeObject(filterContext.ActionParameters);

        //    _logger.AuditLog($"{DateTime.Now.Date} - {user} - {location} - {controller} - {action} - {parameters}");
        //}
    }
}