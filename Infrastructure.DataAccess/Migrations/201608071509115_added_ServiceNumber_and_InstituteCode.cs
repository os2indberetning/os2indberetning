namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class added_ServiceNumber_and_InstituteCode : DbMigration
    {
        public override void Up()
        {
            AddColumn("Employments", "ServiceNumber", c => c.String(unicode: false));
            AddColumn("Employments", "InstituteCode", c => c.String(unicode: false));
        }
        
        public override void Down()
        {
            DropColumn("Employments", "InstituteCode");
            DropColumn("Employments", "ServiceNumber");
        }
    }
}
