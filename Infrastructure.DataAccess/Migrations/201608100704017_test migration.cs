namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class testmigration : DbMigration
    {
        public override void Up()
        {
            DropForeignKey("AppLogins", "PersonId", "People");
            DropIndex("AppLogins", new[] { "PersonId" });
            CreateTable(
                "TempAddressHistories",
                c => new
                    {
                        Id = c.Int(nullable: false, identity: true),
                        AktivFra = c.Long(nullable: false),
                        AktivTil = c.Long(nullable: false),
                        MaNr = c.Int(nullable: false),
                        Navn = c.String(unicode: false),
                        HjemmeAdresse = c.String(unicode: false),
                        HjemmePostNr = c.Int(nullable: false),
                        HjemmeBy = c.String(unicode: false),
                        HjemmeLand = c.String(unicode: false),
                        ArbejdsAdresse = c.String(unicode: false),
                        ArbejdsPostNr = c.Int(nullable: false),
                        ArbejdsBy = c.String(unicode: false),
                        HomeIsDirty = c.Boolean(nullable: false),
                        WorkIsDirty = c.Boolean(nullable: false),
                    })
                .PrimaryKey(t => t.Id)                ;
            
            AddColumn("Addresses", "NextPoint_Id1", c => c.Int());
            CreateIndex("Addresses", "NextPoint_Id1");
            AddForeignKey("Addresses", "NextPoint_Id1", "Addresses", "Id");
            DropColumn("OrgUnits", "DefaultKilometerAllowance");
            DropTable("AppLogins");
        }
        
        public override void Down()
        {
            CreateTable(
                "AppLogins",
                c => new
                    {
                        Id = c.Int(nullable: false, identity: true),
                        UserName = c.String(nullable: false, unicode: false),
                        PersonId = c.Int(nullable: false),
                        Password = c.String(nullable: false, unicode: false),
                        Salt = c.String(nullable: false, unicode: false),
                        GuId = c.String(unicode: false),
                    })
                .PrimaryKey(t => t.Id)                ;
                       AddColumn("OrgUnits", "DefaultKilometerAllowance", c => c.Int(nullable: false));
            DropForeignKey("Addresses", "NextPoint_Id1", "Addresses");
            DropIndex("Addresses", new[] { "NextPoint_Id1" });
            DropColumn("Addresses", "NextPoint_Id1");
            DropTable("TempAddressHistories");
            CreateIndex("AppLogins", "PersonId");
            AddForeignKey("AppLogins", "PersonId", "People", "Id", cascadeDelete: true);
        }
    }
}
