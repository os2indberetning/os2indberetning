namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class DriveReportAddTFCodeOptional : DbMigration
    {
        public override void Up()
        {
            AddColumn("Reports", "TFCodeOptional", c => c.String(unicode: false));
        }
        
        public override void Down()
        {
            DropColumn("Reports", "TFCodeOptional");
        }
    }
}
