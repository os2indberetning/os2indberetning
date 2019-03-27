namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class SubstituteAddTakesOverOriginalLeader : DbMigration
    {
        public override void Up()
        {
            AddColumn("Substitutes", "TakesOverOriginalLeaderReports", c => c.Boolean(nullable: false));
        }
        
        public override void Down()
        {
            DropColumn("Substitutes", "TakesOverOriginalLeaderReports");
        }
    }
}
