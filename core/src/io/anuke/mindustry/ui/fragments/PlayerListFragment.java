package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetConnection;
import io.anuke.mindustry.net.Packets.AdminAction;
import io.anuke.mindustry.ui.BorderImage;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;

import static io.anuke.mindustry.Vars.*;

public class PlayerListFragment extends Fragment{
    private boolean visible = false;
    private Table content = new Table().marginRight(13f).marginLeft(13f);
    private ObjectMap<Player, Boolean> checkmap = new ObjectMap<>();

    @Override
    public void build(Group parent){
        parent.fill(cont -> {
            cont.visible(() -> visible);
            cont.update(() -> {
                if(!(Net.active() && !state.is(State.menu))){
                    visible = false;
                }
                boolean rebuild = false;
                for(Player player : playerGroup.all()){
                    if(!checkmap.containsKey(player) || checkmap.get(player, false) != player.isAdmin){
                        rebuild = true;
                    }
                }
                for(Player player : checkmap.keys()){
                    if(!player.isAdded()){
                        rebuild = true;
                    }
                }
                if(rebuild){
                    rebuild();
                    checkmap.clear();
                    for(Player player : playerGroup.all()){
                        checkmap.put(player, player.isAdmin);
                    }
                }
            });

            cont.table("pane", pane -> {
                pane.label(() -> Bundles.format(playerGroup.size() == 1 ? "text.players.single" : "text.players", playerGroup.size()));
                pane.row();
                pane.pane("clear", content)
                    .grow().get().setScrollingDisabled(true, false);
                pane.row();

                pane.table("pane", menu -> {
                    menu.defaults().growX().height(50f).fillY();

                    menu.addButton("$text.server.bans", ui.bans::show).disabled(b -> Net.client());
                    menu.addButton("$text.server.admins", ui.admins::show).disabled(b -> Net.client());
                    menu.addButton("$text.close", this::toggle);
                }).margin(0f).pad(10f).growX();

            }).touchable(Touchable.enabled).margin(14f);
        });

        rebuild();
    }

    public void rebuild(){
        content.clear();

        float h = 74f;

        for(Player player : playerGroup.all()){
            NetConnection connection = gwt ? null : player.con;

            if(connection == null && Net.server() && !player.isLocal) continue;

            Table button = new Table("button");
            button.left();
            button.margin(5).marginBottom(10);

            Stack stack = new Stack();
            BorderImage image = new BorderImage(Draw.region(player.mech.name), 3f);

            stack.add(image);

            stack.add(new Image(player.mech.iconRegion));

            button.add(stack).size(h);
            button.labelWrap("[#" + player.color.toString().toUpperCase() + "]" + player.name).width(170f).pad(10);
            button.add().grow();

            button.addImage("icon-admin").size(14 * 2).visible(() -> player.isAdmin && !(!player.isLocal && Net.server())).padRight(5);

            if((Net.server() || players[0].isAdmin) && !player.isLocal && (!player.isAdmin || Net.server())){
                button.add().growY();

                float bs = (h + 14) / 2f;

                button.table(t -> {
                    t.defaults().size(bs - 1, bs + 3);
                    //TODO requests.

                    t.addImageButton("icon-ban", 14 * 2, () -> {
                        ui.showConfirm("$text.confirm", "$text.confirmban", () -> Call.onAdminRequest(player, AdminAction.ban));
                    }).padBottom(-5.1f);

                    t.addImageButton("icon-cancel", 14 * 2, () -> Call.onAdminRequest(player, AdminAction.kick)).padBottom(-5.1f);

                    t.row();

                    t.addImageButton("icon-admin", "toggle", 14 * 2, () -> {
                        if(Net.client()) return;

                        String id = netServer.admins.getTraceByID(player.uuid).uuid;

                        if(netServer.admins.isAdmin(id, connection.address)){
                            ui.showConfirm("$text.confirm", "$text.confirmunadmin", () -> {
                                netServer.admins.unAdminPlayer(id);
                            });
                        }else{
                            ui.showConfirm("$text.confirm", "$text.confirmadmin", () -> {
                                netServer.admins.adminPlayer(id, player.usid);
                            });
                        }
                    }).update(b -> {
                        b.setChecked(player.isAdmin);
                        b.setDisabled(Net.client());
                    }).get().setTouchable(() -> Net.client() ? Touchable.disabled : Touchable.enabled);

                    t.addImageButton("icon-zoom-small", 14 * 2, () -> Call.onAdminRequest(player, AdminAction.trace));

                }).padRight(12).padTop(-5).padLeft(0).padBottom(-10).size(bs + 10f, bs);


            }

            content.add(button).padBottom(-6).width(350f).maxHeight(h + 14);
            content.row();
        }

        content.marginBottom(5);
    }

    public void toggle(){
        visible = !visible;
    }

}
