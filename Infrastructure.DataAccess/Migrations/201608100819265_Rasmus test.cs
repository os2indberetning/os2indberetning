namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class Rasmustest : DbMigration
    {
        public override void Up()
        {
            DropForeignKey("Addresses", "NextPoint_Id1", "Addresses");
            DropIndex("Addresses", new[] { "NextPoint_Id1" });
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
                .PrimaryKey(t => t.Id)                
                .ForeignKey("People", t => t.PersonId, cascadeDelete: true)
                .Index(t => t.PersonId);
            
            AddColumn("OrgUnits", "DefaultKilometerAllowance", c => c.Int(nullable: false));
            DropColumn("Addresses", "NextPoint_Id1");
            DropTable("TempAddressHistories");
        }
        
        public override void Down()
        {
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
            DropForeignKey("AppLogins", "PersonId", "People");
            DropIndex("AppLogins", new[] { "PersonId" });
            DropColumn("OrgUnits", "DefaultKilometerAllowance");
            DropTable("AppLogins");
            CreateIndex("Addresses", "NextPoint_Id1");
            AddForeignKey("Addresses", "NextPoint_Id1", "Addresses", "Id");
        }
    }
}
