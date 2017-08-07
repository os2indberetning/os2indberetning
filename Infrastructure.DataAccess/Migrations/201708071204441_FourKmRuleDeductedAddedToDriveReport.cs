namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class FourKmRuleDeductedAddedToDriveReport : DbMigration
    {
        public override void Up()
        {
            AddColumn("Reports", "FourKmRuleDeducted", c => c.Double(false, 0));
        }
        
        public override void Down()
        {
            DropColumn("Reports", "FourKmRuleDeducted");
        }
    }
}
