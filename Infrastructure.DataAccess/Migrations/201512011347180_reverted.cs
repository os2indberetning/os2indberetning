namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class reverted : DbMigration
    {
        public override void Up()
        {
            AddColumn("Addresses", "NextPoint_Id1", c => c.Int());
            CreateIndex("Addresses", "NextPoint_Id1");
            AddForeignKey("Addresses", "NextPoint_Id1", "Addresses", "Id");
        }
        
        public override void Down()
        {
            DropForeignKey("Addresses", "NextPoint_Id1", "Addresses");
            DropIndex("Addresses", new[] { "NextPoint_Id1" });
            DropColumn("Addresses", "NextPoint_Id1");
        }
    }
}
