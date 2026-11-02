/*
 * This is the source code of AyuGram for Android.
 *
 * We do not and cannot prevent the use of our code,
 * but be respectful and credit the original author.
 *
 * Copyright @Radolyn, 2023
 */

package com.radolyn.ayugram.proprietary;

import android.text.TextUtils;
import com.radolyn.ayugram.database.entities.AyuMessageBase;
import com.radolyn.ayugram.database.entities.EditedMessage;
import com.radolyn.ayugram.messages.AyuSavePreferences;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.util.ArrayList;

public class AyuMessageUtils {

    public static void map(AyuSavePreferences prefs, AyuMessageBase entity) {
        if (prefs == null || entity == null) return;
        TLRPC.Message msg = prefs.getMessage();
        entity.userId = prefs.getUserId();
        entity.dialogId = prefs.getDialogId();
        entity.topicId = prefs.getTopicId();
        entity.messageId = prefs.getMessageId();
        entity.entityCreateDate = prefs.getRequestCatchTime();

        if (msg != null) {
            entity.groupedId = msg.grouped_id;
            entity.date = msg.date;
            entity.flags = msg.flags;
            entity.editDate = msg.edit_date;
            entity.views = msg.views;
            entity.text = msg.message;

            if (msg.peer_id != null) {
                entity.peerId = MessageObject.getPeerId(msg.peer_id);
            }
            if (msg.from_id != null) {
                entity.fromId = MessageObject.getPeerId(msg.from_id);
            }

            if (msg.fwd_from != null) {
                entity.fwdFlags = msg.fwd_from.flags;
                entity.fwdDate = msg.fwd_from.date;
                entity.fwdName = msg.fwd_from.from_name;
                entity.fwdPostAuthor = msg.fwd_from.post_author;
                if (msg.fwd_from.from_id != null) {
                    entity.fwdFromId = MessageObject.getPeerId(msg.fwd_from.from_id);
                }
            }

            if (msg.reply_to != null) {
                entity.replyFlags = msg.reply_to.flags;
                entity.replyMessageId = msg.reply_to.reply_to_msg_id;
                entity.replyTopId = msg.reply_to.reply_to_top_id;
                if (msg.reply_to.reply_to_peer_id != null) {
                    entity.replyPeerId = MessageObject.getPeerId(msg.reply_to.reply_to_peer_id);
                }
            }

            if (msg.entities != null && !msg.entities.isEmpty()) {
                try {
                    SerializedData data = new SerializedData(true);
                    data.writeInt32(msg.entities.size());
                    for (TLRPC.MessageEntity e : msg.entities) {
                        e.serializeToStream(data);
                    }
                    entity.textEntities = data.toByteArray();
                } catch (Exception ignored) {}
            }
        }
    }

    public static void mapMedia(AyuSavePreferences prefs, AyuMessageBase entity, boolean saveMedia) {
        if (prefs == null || entity == null) return;
        TLRPC.Message msg = prefs.getMessage();
        if (msg == null || msg.media == null) return;

        try {
            if (msg.media.document != null) {
                entity.documentType = MessageObject.getDocumentType(msg.media.document);
                entity.mimeType = msg.media.document.mime_type;

                SerializedData data = new SerializedData(true);
                msg.media.document.serializeToStream(data);
                entity.documentSerialized = data.toByteArray();
            }

            if (saveMedia) {
                File path = FileLoader.getInstance(prefs.getAccountId()).getPathToMessage(msg);
                if (path != null && path.exists()) {
                    entity.mediaPath = path.getAbsolutePath();
                }
            }
        } catch (Exception ignored) {}
    }

    public static void map(EditedMessage entity, TLRPC.Message msg, int currentAccount) {
        if (entity == null || msg == null) return;
        msg.id = entity.messageId;
        msg.date = entity.date;
        msg.flags = entity.flags;
        msg.edit_date = entity.editDate;
        msg.views = entity.views;
        msg.message = entity.text != null ? entity.text : "";
        msg.dialog_id = entity.dialogId;
        msg.grouped_id = entity.groupedId;

        if (entity.fromId != 0) {
            msg.from_id = MessagesController.getInstance(currentAccount).getPeer(entity.fromId);
        }
        if (entity.peerId != 0) {
            msg.peer_id = MessagesController.getInstance(currentAccount).getPeer(entity.peerId);
        }

        if (entity.textEntities != null && entity.textEntities.length > 0) {
            try {
                SerializedData data = new SerializedData(entity.textEntities);
                int count = data.readInt32(false);
                msg.entities = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    int constructor = data.readInt32(false);
                    TLRPC.MessageEntity me = TLRPC.MessageEntity.TLdeserialize(data, constructor, false);
                    if (me != null) {
                        msg.entities.add(me);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    public static void mapMedia(EditedMessage entity, TLRPC.Message msg) {
        if (entity == null || msg == null) return;
        if (entity.documentSerialized != null && entity.documentSerialized.length > 0) {
            try {
                SerializedData data = new SerializedData(entity.documentSerialized);
                int constructor = data.readInt32(false);
                TLRPC.Document doc = TLRPC.Document.TLdeserialize(data, constructor, false);
                if (doc != null) {
                    TLRPC.TL_messageMediaDocument mediaDoc = new TLRPC.TL_messageMediaDocument();
                    mediaDoc.document = doc;
                    mediaDoc.flags |= 1;
                    msg.media = mediaDoc;
                    msg.flags |= TLRPC.MESSAGE_FLAG_HAS_MEDIA;
                }
            } catch (Exception ignored) {}
        }
    }
}
