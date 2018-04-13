﻿using System.Linq;
using System.Net;
using System.Web.Http;
using System.Web.OData;
using System.Web.OData.Query;
using Core.ApplicationServices;
using Core.ApplicationServices.Interfaces;
using Core.DomainModel;
using Core.DomainServices;
using Microsoft.Ajax.Utilities;
using Core.DomainServices.Interfaces;
using System.Collections.Generic;

namespace OS2Indberetning.Controllers
{
    public class OrgUnitsController : BaseController<OrgUnit>
    {
        private readonly IOrgUnitService _orgService;
        private IPersonService _person;


        public OrgUnitsController(IGenericRepository<OrgUnit> repo, IGenericRepository<Person> personRepo, IOrgUnitService orgService, IPersonService personService) : base(repo, personRepo)
        {
            _orgService = orgService;
            _person = personService;
        }

        //GET: odata/OrgUnits
        /// <summary>
        /// GET API endpoint for OrgUnits
        /// </summary>
        /// <param name="queryOptions"></param>
        /// <returns>OrgUnits</returns>
        [EnableQuery]
        public IQueryable<OrgUnit> Get(ODataQueryOptions<OrgUnit> queryOptions)
        {
            var res =  GetQueryable(queryOptions);
            return res;
        }

        //GET: odata/OrgUnits(5)
        /// <summary>
        /// GET API endpoint for a single OrgUnit
        /// </summary>
        /// <param name="key">Returns the OrgUnit identified by key</param>
        /// <param name="queryOptions"></param>
        /// <returns>A single OrgUnit</returns>
        public IQueryable<OrgUnit> Get([FromODataUri] int key, ODataQueryOptions<OrgUnit> queryOptions)
        {
            return GetQueryable(key, queryOptions);
        }

        //GET: odata/OrgUnits
        /// <summary>
        /// Returns OrgUnits for which the user identified by personId is responsible for approving.
        /// </summary>
        /// <param name="personId"></param>
        /// <returns>OrgUnits</returns>
        [EnableQuery]
        public IHttpActionResult GetWhereUserIsResponsible(int personId)
        {
           return Ok(_orgService.GetWhereUserIsResponsible(personId));
        }

        /// <summary>
        /// Returns the leader of the orgunit specified by orgId
        /// </summary>
        /// <param name="orgId"></param>
        /// <returns>OrgUnits</returns>
        [EnableQuery]
        public IHttpActionResult GetLeaderOfOrg(int orgId)
        {
            return Ok(_orgService.GetLeaderOfOrg(orgId));
        }

        //PUT: odata/OrgUnits(5)
        /// <summary>
        /// Not implemented.
        /// </summary>
        /// <param name="key"></param>
        /// <param name="delta"></param>
        /// <returns></returns>
        public new IHttpActionResult Put([FromODataUri] int key, Delta<OrgUnit> delta)
        {
            return base.Put(key, delta);
        }

        //POST: odata/OrgUnits
        /// <summary>
        /// Not implemented.
        /// </summary>
        /// <param name="orgUnit"></param>
        /// <returns></returns>
        [EnableQuery]
        public new IHttpActionResult Post(OrgUnit orgUnit)
        {
            return StatusCode(HttpStatusCode.MethodNotAllowed);
        }

        //PATCH: odata/OrgUnits(5)
        /// <summary>
        /// PATCH API endpoint for OrgUnits. Returns NotAllowed if the current user is not an admin.
        /// </summary>
        /// <param name="key">Patches the OrgUnit identified by key</param>
        /// <param name="delta"></param>
        /// <returns></returns>
        [EnableQuery]
        [AcceptVerbs("PATCH", "MERGE")]
        public new IHttpActionResult Patch([FromODataUri] int key, Delta<OrgUnit> delta)
        {
            return CurrentUser.IsAdmin ? base.Patch(key, delta) : StatusCode(HttpStatusCode.MethodNotAllowed);
        }

        //DELETE: odata/OrgUnits(5)
        /// <summary>
        /// Not implemented.
        /// </summary>
        /// <param name="key"></param>
        /// <returns></returns>
        public new IHttpActionResult Delete([FromODataUri] int key)
        {
            return StatusCode(HttpStatusCode.MethodNotAllowed);
        }

        /// <summary>
        /// Gets all orgUnits that for the leader
        /// </summary>
        /// <returns></returns>
        [System.Web.Http.HttpGet]
        public IHttpActionResult GetOrgUnitsForLeader()
        {
            List<OrgUnit> orgUnits = new List<OrgUnit>();
            if (CurrentUser.Employments.Where(e => e.IsLeader).Any() || CurrentUser.SubstituteLeaders.Count > 0)
            {
                orgUnits = _person.GetOrgUnitsForLeader(CurrentUser);
            }
            else
            {
                return Unauthorized();
            }
            return Ok(orgUnits);
        }
    }
}