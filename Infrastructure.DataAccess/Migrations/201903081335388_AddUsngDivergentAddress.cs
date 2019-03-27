namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class AddUsngDivergentAddress : DbMigration
    {
        public override void Up()
        {
            AddColumn("Reports", "IsUsingDivergentAddress", c => c.Boolean(nullable: false, defaultValue: false));
        }

        public override void Down()
        {
            DropColumn("Reports", "IsUsingDivergentAddress");
        }
    }
}
