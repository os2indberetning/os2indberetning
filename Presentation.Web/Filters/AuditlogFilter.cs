using Core.ApplicationServices.Logger;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using System.Web.Http.Filters;
using System.Web.Http.Controllers;
using System.Web.OData.Query;
using System.Net.Http;

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
            var user = HttpContext.Current.User.Identity.Name;
            var location = HttpContext.Current.Request.UserHostAddress;

            object controller, action = null;
            actionContext.RequestContext.RouteData.Values.TryGetValue("controller", out controller);
            actionContext.RequestContext.RouteData.Values.TryGetValue("action", out action);

            string jsonParameters = null;
            var parameters = new Dictionary<string, object>(actionContext.ActionArguments);
            var queryOptionsDictionaryEntry = parameters.Where(x => x.Value is ODataQueryOptions).FirstOrDefault();
            try
            {
                var queryOptions = queryOptionsDictionaryEntry.Value as ODataQueryOptions;
                Dictionary<string, string> rawValues = new Dictionary<string, string>();
                rawValues.Add("Count", queryOptions.RawValues.Count);
                rawValues.Add("Expand", queryOptions.RawValues.Expand);
                rawValues.Add("Filter", queryOptions.RawValues.Filter);
                rawValues.Add("Format", queryOptions.RawValues.Format);
                rawValues.Add("OrderBy", queryOptions.RawValues.OrderBy);
                rawValues.Add("Select", queryOptions.RawValues.Select);
                rawValues.Add("Skip", queryOptions.RawValues.Skip);
                rawValues.Add("SkipToken", queryOptions.RawValues.SkipToken);
                rawValues.Add("Top", queryOptions.RawValues.Top);

                parameters[queryOptionsDictionaryEntry.Key] = rawValues;
                jsonParameters = JsonConvert.SerializeObject(parameters);
            }
            catch (Exception)
            {
                // No paramater of type ODataQueryOptions was found in actionContext.ActionArguments, which is fine.
            }

            try
            {
                _logger.AuditLog(user, location, controller?.ToString(), action?.ToString(), jsonParameters);
            }
            catch (Exception e)
            {
                _logger.Error($"{GetType().Name}, OnActionExecuting(), Auditlogging failed.", e);

                // By setting the Response, the filter returns the call without finished the original request. User should not be able to invoke actions if it can not be auditlogged.
                actionContext.Response = actionContext.Request.CreateErrorResponse(System.Net.HttpStatusCode.InternalServerError, "Auditlogging failed");
            }

            base.OnActionExecuting(actionContext);
        }
    }
}