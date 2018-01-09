namespace Infrastructure.DataAccess.Migrations
{
    using System;
    using System.Data.Entity.Migrations;
    
    public partial class AddedAdminReceiveMail : DbMigration
    {
        public override void Up()
        {
            AddColumn("People", "AdminRecieveMail", c => c.Boolean(nullable: false, defaultValue: false));
            Sql("UPDATE People SET AdminRecieveMail = IsAdmin");
        }
        
        public override void Down()
        {
            DropColumn("People", "AdminRecieveMail");
        }
    }
}
