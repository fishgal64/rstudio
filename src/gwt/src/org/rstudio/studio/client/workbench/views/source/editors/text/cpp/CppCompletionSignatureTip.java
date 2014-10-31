package org.rstudio.studio.client.workbench.views.source.editors.text.cpp;

import java.util.ArrayList;

import org.rstudio.core.client.Rectangle;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay.AnchoredSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;
import org.rstudio.studio.client.workbench.views.source.model.CppCompletion;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;

public class CppCompletionSignatureTip extends CppCompletionToolTip
{
   public CppCompletionSignatureTip(CppCompletion completion, 
                                    DocDisplay docDisplay)
   {
      // save references
      completion_ = completion;
      docDisplay_ = docDisplay;
      
      // save cursor bounds
      cursorBounds_ = docDisplay_.getCursorBounds();
  
      // create an anchored selection to track editing
      Position start = docDisplay_.getSelectionStart();
      start = Position.create(start.getRow(), start.getColumn() - 1);
      Position end = docDisplay_.getSelectionEnd();
      end = Position.create(end.getRow(), end.getColumn() + 1);
      anchor_ = docDisplay_.createAnchoredSelection(start, end);
     
      // set the max width
      setMaxWidth(Window.getClientWidth() - 200);
      
      // add paging widget if necessary
      if (completion.getText().length() > 1)
         addPagingWidget();
      
      // set initial text
      setTextIndex(0);
   }
   
   private void addPagingWidget()
   {
      HorizontalPanel panel = new HorizontalPanel();
      panel.setStyleName(RES.styles().pagingWidget());
      
      Image upImage = new Image(RES.upArrow());
      upImage.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            setTextIndex(currentTextIndex_ - 1);  
            docDisplay_.setFocus(true);
         }
      });
      panel.add(upImage);
      
      pagingLabel_ = new Label();
      panel.add(pagingLabel_);
      
      Image downImage = new Image(RES.downArrow());
      downImage.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            setTextIndex(currentTextIndex_ + 1);  
            docDisplay_.setFocus(true);
         }
      });
      panel.add(downImage);
      panel.add(downImage);
   
      addLeftWidget(panel);
   }
   
   private void setTextIndex(int index)
   {
      if (index >= 0 && index < completion_.getText().length())
      {
         currentTextIndex_ = index;
         
         if (pagingLabel_ != null)
         {
            pagingLabel_.setText((index+1) + " of " + 
                                 completion_.getText().length());
         }
         
         setText(completion_.getText().get(currentTextIndex_));
         
         setPopupPositionAndShow(new PositionCallback() {
            @Override
            public void setPosition(int offsetWidth, int offsetHeight)
            {
               // determine left and top
               final int H_PAD = 3;
               final int V_PAD = 5;
               final int MARGIN = 50;
               
               // we only calculate left one time
               if (left_ == null)
               {
                  int left = cursorBounds_.getLeft() + H_PAD;
                  
                  // do we have enough horizontal space? if not then shift left
                  int spaceRight = Window.getClientWidth() - 
                                   offsetWidth - left - (3*MARGIN);
                  if (spaceRight < 0)
                     left += spaceRight;
                  
                  left_ = left;
                  
                  // update max width
                  setMaxWidth(Window.getClientWidth() - left_ - MARGIN);
                  
               }
               
               // do we have enough vertical space? if not then show at bottom
               int top = cursorBounds_.getTop() - offsetHeight - V_PAD;
               int spaceTop = top - offsetHeight - MARGIN;
               if (spaceTop < 0)
               {
                  top = cursorBounds_.getTop() + 
                        cursorBounds_.getHeight() 
                        + (V_PAD * 2);
               }
               
               
               setPopupPosition(left_, top); 
            }
         }); 
      }
   }
   
   public static void hideAll()
   {
      // clone so we aren't interacting with the list while we
      // are iterating over it
      ArrayList<CppCompletionSignatureTip> allTips = 
                              new ArrayList<CppCompletionSignatureTip>();
      allTips.addAll(allTips_);
      for (CppCompletionSignatureTip tip : allTips)
         tip.hide();
   }
   
   @Override
   protected void onUnload()
   {
      allTips_.remove(this);
      nativePreviewReg_.removeHandler();
      super.onUnload();
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();
      allTips_.add(this);
      nativePreviewReg_ = Event.addNativePreviewHandler(new NativePreviewHandler()
      {
         public void onPreviewNativeEvent(NativePreviewEvent e)
         {
            if (e.getTypeInt() == Event.ONKEYDOWN)
            {
               // handle keys
               switch (e.getNativeEvent().getKeyCode())
               {
                  case KeyCodes.KEY_ESCAPE:
                     e.cancel();
                     hide();
                     break;
                  case KeyCodes.KEY_DOWN:
                     e.cancel();
                     setTextIndex(currentTextIndex_ + 1);
                     break;
                  case KeyCodes.KEY_UP:
                     e.cancel();
                     setTextIndex(currentTextIndex_ - 1);
                     break;
               }
               
               // dismiss if we've left our anchor zone
               // (defer this so the current key has a chance to 
               // enter the editor and affect the cursor)
               Scheduler.get().scheduleDeferred(new ScheduledCommand() {

                  @Override
                  public void execute()
                  {
                     Position cursorPos = docDisplay_.getCursorPosition();
                     Range anchorRange = anchor_.getRange();
                     if (cursorPos.isBeforeOrEqualTo(anchorRange.getStart()) ||
                         cursorPos.isAfterOrEqualTo(anchorRange.getEnd()))
                     {
                        hide();
                     }
                  }
               });
            }
         }
      });
   }
   
   private final CppCompletion completion_;
   private int currentTextIndex_;
   private Label pagingLabel_;
   private Integer left_ = null;
   private final Rectangle cursorBounds_;
   private final AnchoredSelection anchor_;
   private final DocDisplay docDisplay_;
   private HandlerRegistration nativePreviewReg_;
   
   private static ArrayList<CppCompletionSignatureTip> allTips_ = 
         new ArrayList<CppCompletionSignatureTip>();
   
   private static final CppCompletionResources RES = 
                                       CppCompletionResources.INSTANCE;
}