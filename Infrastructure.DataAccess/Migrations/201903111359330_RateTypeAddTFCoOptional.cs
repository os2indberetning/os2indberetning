namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class RateTypeAddTFCoOptional : DbMigration
    {
        public override void Up()
        {
            AddColumn("RateTypes", "TFCodeOptional", c => c.String(unicode: false, nullable: true));
        }
        
        public override void Down()
        {
            DropColumn("RateTypes", "TFCodeOptional");
        }
    }
}
