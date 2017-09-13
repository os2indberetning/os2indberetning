namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class FourKmRuleDeductedAddedToDriveReport : DbMigration
    {
        public override void Up()
        {
            AddColumn("Reports", "FourKmRuleDeducted", c => c.Double(false, 0));
            Sql("UPDATE reports SET FourKmRuleDeducted = 4 WHERE FourKmRule = 1");
        }
        
        public override void Down()
        {
            DropColumn("Reports", "FourKmRuleDeducted");
        }
    }
}
