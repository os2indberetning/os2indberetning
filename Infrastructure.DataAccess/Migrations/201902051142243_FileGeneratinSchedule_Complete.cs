namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class FileGeneratinSchedule_Complete : DbMigration
    {
        public override void Up()
        {
            AddColumn("FileGenerationSchedules", "Completed", c => c.Boolean(nullable: false, defaultValue: true));
        }
        
        public override void Down()
        {
            DropColumn("FileGenerationSchedules", "Completed");
        }
    }
}
