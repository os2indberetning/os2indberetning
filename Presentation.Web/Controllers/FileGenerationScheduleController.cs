using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Web;
using System.Web.Http;
using System.Web.OData;
using System.Web.OData.Query;
using Core.DomainModel;
using Core.DomainServices;

namespace OS2Indberetning.Controllers
{
    public class FileGenerationsController : BaseController<FileGenerationSchedule>
    {
        public FileGenerationsController(IGenericRepository<FileGenerationSchedule> repository, IGenericRepository<Person> personRepo) : base(repository, personRepo)      {}

        //GET: odata/FileGenerationSchedules
        /// <summary>
        /// GET API for FileGenerations
        /// </summary>
        /// <param name="queryOptions"></param>
        /// <returns>FileGenerationSchedules</returns>
        [EnableQuery]
        public IQueryable<FileGenerationSchedule> Get(ODataQueryOptions<FileGenerationSchedule> queryOptions)
        {
            var res = GetQueryable(queryOptions);
            return res;
        }

        //GET: odata/FileGenerationSchedule(5)
        /// <summary>
        /// GET API endpoint for a single FileGenerationSchedule
        /// </summary>
        /// <param name="key"></param>
        /// <param name="queryOptions"></param>
        /// <returns>A single FileGenereationSchedule</returns>
        public IQueryable<FileGenerationSchedule> Get([FromODataUri] int key, ODataQueryOptions<FileGenerationSchedule> queryOptions)
        {
            return GetQueryable(key, queryOptions);
        }

        //PUT: odata/FileGenerationSchedules(5)
        /// <summary>
        /// 
        /// </summary>
        /// <param name="key"></param>
        /// <param name="delta"></param>
        /// <returns></returns>
        public new IHttpActionResult Put([FromODataUri] int key, Delta<FileGenerationSchedule> delta)
        {
            return base.Put(key, delta);
        }

        //POST: odata/FileGenerationSchedules
        /// <summary>
        /// POST API endpoint for FileGenerationSchedules
        /// Returns forbidden if the current user is not an admin
        /// </summary>
        /// <param name="FileGenerationSchedule">FileGenerationSchedule to be posted</param>
        /// <returns></returns>
        [EnableQuery]
        public new IHttpActionResult Post(FileGenerationSchedule FileGenerationSchedule)
        {
            return CurrentUser.IsAdmin ? base.Post(FileGenerationSchedule) : StatusCode(HttpStatusCode.Forbidden);
        }

        //PATCH: odata/FileGenerationSchedules(5)
        /// <summary>
        /// PATCH API endpoint for FileGenerationSchedule
        /// </summary>
        /// <param name="key">Patches the FileGenerationSchedule identified by key</param>
        /// <param name="delta"></param>
        /// <returns>Returns forbidden if the current user is not an admin</returns>
        [EnableQuery]
        [AcceptVerbs("PATCH", "MERGE")]
        public new IHttpActionResult Patch([FromODataUri] int key, Delta<FileGenerationSchedule> delta)
        {
            return CurrentUser.IsAdmin ? base.Patch(key, delta) : StatusCode(HttpStatusCode.Forbidden);
        }

        //DELETE: odata/FileGenerationShcedule(5)
        /// <summary>
        /// DELETE API endpoint for FileGenerationSchedule.
        /// Returns forbidden if the current user is not an admin.
        /// </summary>
        /// <param name="key">Deletes FileGenerationSchedule identified by the key</param>
        /// <returns></returns>
        public new IHttpActionResult Delete([FromODataUri] int key)
        {
            return CurrentUser.IsAdmin ? base.Delete(key) : StatusCode(HttpStatusCode.Forbidden);
        }

    }
}