package com.tolocharadio.domain.notification

import android.os.Parcel
import android.os.Parcelable

/**
 * Represents the data structure for a notification.
 */
data class NotificationData(
    /** Type of notification (PLAYBACK, CONTENT, SYSTEM) */
    val type: NotificationType,
    
    /** Notification title (public information only) */
    val title: String,
    
    /** Notification message (public information only) */
    val message: String,
    
    /** Optional ID of the content to navigate to */
    val contentId: String? = null,
    
    /** Action to perform when notification is tapped */
    val action: NotificationAction,
    
    /** When the notification was created */
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {
    
    /**
     * Validates the notification data.
     * @return true if the data is valid, false otherwise
     */
    fun isValid(): Boolean {
        return title.isNotBlank() && 
               message.isNotBlank() && 
               (contentId == null || contentId.isNotBlank()) &&
               timestamp > 0
    }
    
    override fun describeContents(): Int = 0
    
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeSerializable(type)
        dest.writeString(title)
        dest.writeString(message)
        dest.writeString(contentId)
        dest.writeSerializable(action)
        dest.writeLong(timestamp)
    }
    
    companion object CREATOR : Parcelable.Creator<NotificationData> {
        override fun createFromParcel(parcel: Parcel): NotificationData {
            return NotificationData(
                type = parcel.readSerializable() as NotificationType,
                title = parcel.readString() ?: "",
                message = parcel.readString() ?: "",
                contentId = parcel.readString(),
                action = parcel.readSerializable() as NotificationAction,
                timestamp = parcel.readLong()
            )
        }
        
        override fun newArray(size: Int): Array<NotificationData?> = arrayOfNulls(size)
    }
}
