namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class AddedIDMFields : DbMigration
    {
        public override void Up()
        {
            AddColumn("Employments", "ServiceNumber", c => c.String(nullable: true));
            AddColumn("Employments", "InstituteCode", c => c.String(nullable: true));
            AddColumn("OrgUnits", "OrgOUID", c => c.String(nullable: true));
        }
        
        public override void Down()
        {
            DropColumn("OrgUnits", "OrgOUID");
            DropColumn("Employments", "InstituteCode");
            DropColumn("Employments", "ServiceNumber");
        }
    }
}
