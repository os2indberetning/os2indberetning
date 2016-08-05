namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class add_OUID_on_orgUnit : DbMigration
    {
        public override void Up()
        {
            AddColumn("OrgUnits", "OrgOUID", c => c.String(unicode: false));
        }
        
        public override void Down()
        {
            DropColumn("OrgUnits", "OrgOUID");
        }
    }
}
