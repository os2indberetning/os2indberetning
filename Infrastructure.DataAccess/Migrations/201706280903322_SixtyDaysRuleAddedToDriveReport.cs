namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class SixtyDaysRuleAddedToDriveReport : DbMigration
    {
        public override void Up()
        {
            AddColumn("Reports", "SixtyDaysRule", c => c.Boolean(false, false));
        }
        
        public override void Down()
        {
            DropColumn("Reports", "SixtyDaysRule");
        }
    }
}
