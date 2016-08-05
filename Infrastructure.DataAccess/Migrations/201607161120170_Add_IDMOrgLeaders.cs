namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class Add_IDMOrgLeaders : DbMigration
    {
        public override void Up()
        {
            CreateTable(
                "IDMOrgLeaders",
                c => new
                    {
                        Id = c.Int(nullable: false, identity: true),
                        OUID = c.String(unicode: false),
                        Leder = c.String(unicode: false),
                    })
                .PrimaryKey(t => t.Id)                ;
            
        }
        
        public override void Down()
        {
            DropTable("IDMOrgLeaders");
        }
    }
}
