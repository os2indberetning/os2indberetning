namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class HomeToBorderDistanceAddedToDriveReport : DbMigration
    {
        public override void Up()
        {
            AddColumn("Reports", "HomeToBorderDistance", c => c.Double());
        }
        
        public override void Down()
        {
            DropColumn("Reports", "HomeToBorderDistance");
        }
    }
}
