namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class HomeToBorderDistanceAddedToDriveReport : DbMigration
    {
        public override void Up()
        {
            AddColumn("Reports", "HomeToBorderDistance", c => c.Double(nullable: false, defaultValue: 0));
        }
        
        public override void Down()
        {
            DropColumn("Reports", "HomeToBorderDistance");
        }
    }
}
