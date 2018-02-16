namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class AdminRecieveMailAdded : DbMigration
    {
        public override void Up()
        {
            AddColumn("People", "AdminRecieveMail", c => c.Boolean(nullable: false));
            Sql("UPDATE People SET AdminRecieveMail = IsAdmin");
        }
        
        public override void Down()
        {
            DropColumn("People", "AdminRecieveMail");
        }
    }
}
