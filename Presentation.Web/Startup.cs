using System.Data.Entity;
using Microsoft.Owin;
using Owin;

[assembly: OwinStartup(typeof(OS2Indberetning.Startup))]

namespace OS2Indberetning
{
    public partial class Startup
    {
        public void Configuration(IAppBuilder app)
        {
            DbConfiguration.SetConfiguration(new MySql.Data.Entity.MySqlEFConfiguration());

#if DEBUG
            app.Use((context, next) =>
            {
                context.Response.Headers["Cache-Control"] = "no-cache, no-store, must-revalidate";
                context.Response.Headers["Pragma"] = "no-cache";
                context.Response.Headers["Expires"] = "0";

                return next.Invoke();
            });
#endif

            ConfigureAuth(app);

        }
    }
}
