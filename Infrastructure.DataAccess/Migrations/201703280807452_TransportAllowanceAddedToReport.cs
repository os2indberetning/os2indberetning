namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class TransportAllowanceAddedToReport : DbMigration
    {
        public override void Up()
        {
            AddColumn("Reports", "TransportAllowance", c => c.Double(nullable: true, defaultValue: 0));
        }
        
        public override void Down()
        {
            DropColumn("Reports", "TransportAllowance");
        }
    }
}
