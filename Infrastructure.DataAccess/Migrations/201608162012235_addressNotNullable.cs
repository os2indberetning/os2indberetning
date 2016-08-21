namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class addressNotNullable : DbMigration
    {
        public override void Up()
        {
            DropForeignKey("OrgUnits", "AddressId", "Addresses");
            DropIndex("OrgUnits", new[] { "AddressId" });
            AlterColumn("OrgUnits", "AddressId", c => c.Int(nullable: false));
            CreateIndex("OrgUnits", "AddressId");
            AddForeignKey("OrgUnits", "AddressId", "Addresses", "Id", cascadeDelete: true);
        }
        
        public override void Down()
        {
            DropForeignKey("OrgUnits", "AddressId", "Addresses");
            DropIndex("OrgUnits", new[] { "AddressId" });
            AlterColumn("OrgUnits", "AddressId", c => c.Int());
            CreateIndex("OrgUnits", "AddressId");
            AddForeignKey("OrgUnits", "AddressId", "Addresses", "Id");
        }
    }
}
